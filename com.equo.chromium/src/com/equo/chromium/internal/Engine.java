/****************************************************************************
**
** Copyright (C) 2022 Equo
**
** This file is part of Equo Chromium.
**
** Commercial License Usage
** Licensees holding valid commercial Equo licenses may use this file in
** accordance with the commercial license agreement provided with the
** Software or, alternatively, in accordance with the terms contained in
** a written agreement between you and Equo. For licensing terms
** and conditions see https://www.equo.dev/terms.
**
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3 as published by the Free Software
** Foundation. Please review the following
** information to ensure the GNU General Public License requirements will
** be met: https://www.gnu.org/licenses/gpl-3.0.html.
**
****************************************************************************/


package com.equo.chromium.internal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cef.CefApp;
import org.cef.CefAppStandalone;
import org.cef.CefAppSwt;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.SystemBootstrap;
import org.cef.SystemBootstrap.Loader;
import org.cef.WindowingToolkit;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandlerAdapter;

import com.equo.chromium.swt.internal.SWTEngine;
import com.equo.chromium.swt.internal.spi.DynamicCefSchemeHandlerFactory;
import com.equo.chromium.swt.internal.spi.SchemeDomainPair;
import com.equo.chromium.swt.internal.spi.SchemeHandler;
import com.equo.chromium.swt.internal.spi.SchemeHandlerManager;
import com.equo.chromium.swt.internal.spi.StaticCefSchemeHandlerFactory;

public class Engine {
	static {
		loadLib();
	}
	private static final String CEFVERSION = "5249";
	private static final String SUBDIR = "chromium-" + CEFVERSION;
	private static final String SCHEME_FILE = "file"; //$NON-NLS-1$
	private static Path libsPath;

	public static final boolean debug = Boolean.valueOf(System.getProperty("chromium.debug", "false"));

	private static AtomicBoolean shuttingDown = new AtomicBoolean();
	private static List<SchemeDomainPair> registeredSchemeData;

	private static CefApp app;
	public static final CompletableFuture<Boolean> ready = new CompletableFuture<>();
	private static AtomicBoolean closing = new AtomicBoolean();
	private static boolean multiThreaded;

	private static void loadLib() {
		if (OS.isLinux()) {
			multiThreaded = System.getProperty("chromium.multi_threaded_message_loop") != null ? Boolean.getBoolean("chromium.multi_threaded_message_loop") : false;
			if (multiThreaded && Boolean.valueOf(System.getProperty("chromium.debug", "false")))
				System.out.println("J: multi_threaded_message_loop enabled");
		}

		libsPath = findLibsPath().resolve(SUBDIR);
		SystemBootstrap.setLoader(new Loader() {
			@Override
			public void loadLibrary(String libname) {
				System.load(libsPath.resolve(System.mapLibraryName(libname)).toString());
			}
		});
		if (!Files.exists(libsPath))
			throw new RuntimeException("Missing binaries for Equo Chromium Browser.");
		boolean checkGtkInit = checkGtkInit();
		String[] args = getChromiumArgs(libsPath, Boolean.getBoolean("chromium.init_threads"), checkGtkInit);
		if (!CefApp.startup(args)) {
			if (checkGtkInit) {
				throw new RuntimeException("To run Chromium on Wayland, set env var GDK_BACKEND=x11 or call ChromiumBrowser.earlyInit() before creating a window");
			}
			throw new RuntimeException("Failed to load binaries for Equo Chromium Browser.");
		}
	}

	private static boolean checkGtkInit() {
		if (!OS.isLinux()) 
			return false;
		String backend = System.getenv("GDK_BACKEND");
		String session = System.getenv("XDG_SESSION_TYPE");
		boolean checkGtkInit = "wayland".equals(session) && !"x11".equals(backend);
		return checkGtkInit;
	}

	private static Path findLibsPath() {
		String chromiumPath = System.getProperty("chromium.path", "");
		if (!chromiumPath.isEmpty() && Files.exists(Paths.get(chromiumPath, SUBDIR).toAbsolutePath()))
			return Paths.get(chromiumPath).toAbsolutePath().normalize();
		String arch = getArch();
		String binariesBsn = "com.equo.chromium.cef." + Utils.getWindowing() + "." + Utils.getOS() + "." + arch;
		try {
			Class<?> fragmentClass = Class.forName("com.equo.chromium.ChromiumFragment");
			CodeSource codeSource = fragmentClass.getProtectionDomain().getCodeSource();
			if (codeSource != null) {
				URI loc = toURI(codeSource.getLocation());
				Path fragment = Paths.get(loc).toAbsolutePath().normalize();
				try {
					if (Files.isRegularFile(fragment)) {
						Path extractPath = ResourceExpander.extractFromJar(chromiumPath, arch, SUBDIR, fragmentClass);
						if (extractPath != null)
							return extractPath;
					}
					// tycho surefire explodes the jar but does not set files executables.
					Files.walkFileTree(fragment, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							ResourceExpander.setExecutable(file.toFile());
							return super.visitFile(file, attrs);
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
				return fragment;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("plugin/jar '" + binariesBsn
				+ "' is missing and system property 'chromium.path' is not correctly set.");
	}

	/**
	 * Returns the URL as a URI. This method will handle URLs that are not properly
	 * encoded (for example they contain unencoded space characters).
	 * 
	 * @param url The URL to convert into a URI
	 * @return A URI representing the given URL
	 * @throws UnsupportedEncodingException 
	 */
	private static URI toURI(URL url) throws URISyntaxException, UnsupportedEncodingException {
		// URL behaves differently across platforms so for file: URLs we parse from
		// string form
		if (SCHEME_FILE.equals(url.getProtocol())) {
			String pathString = url.toExternalForm().substring(5);
			// ensure there is a leading slash to handle common malformed URLs such as
			// file:c:/tmp
			if (pathString.indexOf('/') != 0)
				pathString = '/' + pathString;
			return new URI(SCHEME_FILE, null, URLDecoder.decode(pathString, "UTF-8"), null);
		}
		try {
			return new URI(url.toExternalForm());
		} catch (URISyntaxException e) {
			// try multi-argument URI constructor to perform encoding
			return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
					url.getQuery(), url.getRef());
		}
	}

	public static void initCEF() {
		initCEF(false);
	}

	static void initCEF(boolean standalone) {
		synchronized (Engine.class) {
			if (app == null) {
				CefSettings settings = new CefSettings();
				try {
					settings.remote_debugging_port = Integer
							.parseInt(System.getProperty("chromium.remote_debugging_port", "0"));
				} catch (NumberFormatException e) {
				}
				Path data = Paths.get(System.getProperty("chromium.home", System.getProperty("user.home")), ".equo");
				String cache = data.resolve("cefcache").toAbsolutePath().toString();
				settings.cache_path = System.getProperty("chromium.cache_path", cache);
				settings.log_file = data.resolve("cef.log").toString();
				settings.log_severity = debug ? CefSettings.LogSeverity.LOGSEVERITY_INFO
						: CefSettings.LogSeverity.LOGSEVERITY_DISABLE;

				boolean external_message_pump = true;
				if (OS.isMacintosh()) {
					settings.browser_subprocess_path = libsPath.resolve("equochro Helper.app/Contents/MacOS/equochro Helper")
							.toString();
					settings.resources_dir_path = libsPath.resolve("Chromium Embedded Framework.framework")
							.resolve("Resources").toString();
				} else if (OS.isWindows()) {
					settings.browser_subprocess_path = libsPath.resolve("equochro_helper.exe").toString();
					settings.resources_dir_path = libsPath.toString();
					settings.locales_dir_path = libsPath.resolve("locales").toString();
					settings.multi_threaded_message_loop = System.getProperty("chromium.multi_threaded_message_loop") != null ? Boolean.getBoolean("chromium.multi_threaded_message_loop") : false;
				} else if (OS.isLinux()) {
					settings.browser_subprocess_path = libsPath.resolve("equochro_helper").toString();
					settings.resources_dir_path = libsPath.toString();
					settings.locales_dir_path = libsPath.resolve("locales").toString();
					settings.multi_threaded_message_loop = multiThreaded;
					external_message_pump = false;
				}
				settings.external_message_pump = !settings.multi_threaded_message_loop && (System.getProperty("chromium.external_message_pump") != null
						? Boolean.getBoolean("chromium.external_message_pump")
						: external_message_pump);

				String[] args = getChromiumArgs(libsPath, false, false);

				final SchemeHandlerManager schemeHandlerManager = SchemeHandlerManager.get();

				if (schemeHandlerManager != null) {
					registeredSchemeData = schemeHandlerManager.getRegisteredSchemes();
				} else {
					registeredSchemeData = Collections.emptyList();
				}

				int loopTime = (!settings.external_message_pump && !registeredSchemeData.isEmpty()) ? 1000 / 180 : WindowingToolkit.DEFAULT_LOOP_TIME;
				WindowingToolkit windowToolkit = null;
				if (standalone) {
					windowToolkit = new CefAppStandalone();
					settings.multi_threaded_message_loop = false;
					settings.external_message_pump = false;
				} else {
					windowToolkit = new CefAppSwt(loopTime, settings.external_message_pump);
				}
				CefApp.setWindowingToolkit(windowToolkit);

				CefApp.addAppHandler(new CefAppHandlerAdapter(args) {
					@Override
					public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
						if (!registeredSchemeData.isEmpty()) {
							for (SchemeDomainPair schemeDomain : registeredSchemeData) {
								String scheme = schemeDomain.getScheme();
								registrar.addCustomScheme(scheme, true, false, false, true, true, false, true);
							}
						}
					}

					@Override
					public void onScheduleMessagePumpWork(long delay_ms) {
						if (shuttingDown.get()) {
							debug("Ignoring onScheduleMessagePumpWork due shuttingDown");
							return;
						}
						super.onScheduleMessagePumpWork(delay_ms);
					}

					@Override
					public void onContextInitialized() {
						if (!registeredSchemeData.isEmpty() && app != null) {
							for (final SchemeDomainPair schemeData : registeredSchemeData) {
								SchemeHandler schemeHandler = schemeHandlerManager
										.getSchemeHandler(schemeData.getScheme(), schemeData.getDomain());
								app.registerSchemeHandlerFactory(schemeData.getScheme(), schemeData.getDomain(),
										schemeHandler == null ? new DynamicCefSchemeHandlerFactory(schemeHandlerManager)
												: new StaticCefSchemeHandlerFactory(schemeHandlerManager, schemeData));
							}
						}
						if (!standalone) {
							SWTEngine.onContextInitialized(app);
						}
						ready.complete(true);
					}

					@Override
					public boolean onBeforeTerminate() {
						if (shuttingDown.get()) {
							// already shutdown
							return false;
						}
						shuttingDown.set(true);
						internalShutdown();
						return true;
					}
				});

				try {
					app = CefApp.getInstance(settings);
				} catch (UnsatisfiedLinkError e) {
					if ("gtk".equals(Utils.getWindowing()) && e.getMessage().contains("libgconf")) {
						System.load(libsPath.resolve("libgconf-2.so.4").toString());
						app = CefApp.getInstance(settings);
					} else {
						throw e;
					}
				}
				if (!standalone) {
					SWTEngine.initCef(closing, shuttingDown, () -> internalShutdown());
				}

			}
		}
	}

	static String[] getChromiumArgs(Path libsPath, boolean addXInitThreads, boolean addGtkInitCheck) {
		List<String> args = new ArrayList<>();
		String vmArg = System.getProperty("chromium.args", System.getProperty("swt.chromium.args"));
		if (vmArg != null) {
			String[] lines = vmArg.replace("\\;", "\\#$").split(";");
			Arrays.stream(lines).map(line -> line.replace("\\#$", ";")).forEach(l -> args.add(l));
		}
		if (OS.isLinux()) {
			args.add("--disable-gpu-compositing");
			if (addXInitThreads)
				args.add("XInitThreads");
			if (addGtkInitCheck)
				args.add("GTKInitCheck");
		} else if (OS.isMacintosh()) {
			args.add("--framework-dir-path=" + libsPath.resolve("Chromium Embedded Framework.framework"));
			args.add("--main-bundle-path=" + libsPath.resolve("equochro Helper.app"));
		}
		return args.toArray(new String[args.size()]);
	}

	private static void internalShutdown() {
		if (app == null) {
			return;
		}
		app.dispose();
		app = null;
	}

	public static <T extends CefClient> T createClient() {
		return app.createClient();
	}

	public static void startCefLoop() {
		app.runMessageLoop();
	}

	private static String getArch() {
		String osArch = System.getProperty("os.arch");
		if (osArch.equals("i386") || osArch.equals("i686"))
			return "x86";
		if (osArch.equals("amd64"))
			return "x86_64";
		return osArch;
	}

	public static void debug(String log) {
		if (debug) {
			System.out.println("J:" + log);
		}
	}

	public static void debug(String log, CefBrowser cefBrowser) {
		if (debug) {
			int identifier = cefBrowser != null ? cefBrowser.getIdentifier() : -1;
			System.out.println(System.currentTimeMillis() / 1000 + ":J" + identifier + ":"
					+ Thread.currentThread().getName() + ":" + log);
		}
	}

	public static boolean isRegisteredProtocol(String url) {
		if (registeredSchemeData != null) {
			for (SchemeDomainPair schemeDomain : registeredSchemeData) {
				String scheme = schemeDomain.getScheme();
				if (url.startsWith(scheme)) {
					return true;
				}
			}
		}
		return false;
	}
}
