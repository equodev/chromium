/****************************************************************************
**
** Copyright (C) 2026 Equo
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

import static com.equo.chromium.internal.ResourceExpander.CUSTOM_EXECUTABLE_NAME;
import static com.equo.chromium.internal.ResourceExpander.DEFAULT_EXECUTABLE_NAME;
import static com.equo.chromium.internal.ResourceExpander.createExecutableWithCustomName;
import static com.equo.chromium.internal.ResourceExpander.isHelperExecutable;
import static com.equo.chromium.internal.Utils.getWindowing;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefAppStandalone;
import org.cef.CefAppSwing;
import org.cef.CefAppSwt;
import org.cef.CefClient;
import org.cef.CefColor;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.SystemBootstrap;
import org.cef.SystemBootstrap.Loader;
import org.cef.WindowingToolkit;
import org.cef.callback.CefCommandLine;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandlerAdapter;

import com.equo.chromium.swt.internal.SWTEngine;
import com.equo.chromium.swt.internal.spi.DynamicCefSchemeHandlerFactory;
import com.equo.chromium.swt.internal.spi.MiddlewareResourceRequestHandler;
import com.equo.chromium.swt.internal.spi.SchemeDomainPair;
import com.equo.chromium.swt.internal.spi.SchemeHandler;
import com.equo.chromium.swt.internal.spi.SchemeHandlerManager;
import com.equo.chromium.swt.internal.spi.SecurityManager;
import com.equo.chromium.swt.internal.spi.StaticCefSchemeHandlerFactory;
import static com.equo.chromium.swt.Log.debug;

public class Engine {
	static {
		loadLib();
	}
	public static final String CEFVERSION = "7871";
	private static final String SUBDIR = "chromium-" + CEFVERSION;
	private static final String SCHEME_FILE = "file"; //$NON-NLS-1$
	private static final String BUNDLE_SYMBOLIC_NAME = "com.equo.chromium";
	private static Path libsPath;
	private static AtomicBoolean shuttingDown = new AtomicBoolean();
	public static MiddlewareResourceRequestHandler middlewareResourceRequestHandler;

	private static CefApp app;
	public static final CompletableFuture<Boolean> ready = new CompletableFuture<>();
	private static AtomicBoolean closing = new AtomicBoolean();
	private static boolean multiThreaded;
	private static Path rootCacheFolder = null;
	public static boolean setDarkModeBackground = false;
	private static java.io.RandomAccessFile lockRaf = null;
	private static java.nio.channels.FileLock cacheLock = null;
	private static boolean shutdownHookRegistered = false;
	private static Path tempUserDataPath = null;

	private static FileSystem fileSystem = FileSystems.getDefault();

	public static enum BrowserType {
		SWT, STANDALONE, SWING, HEADLESS
	}

	public static enum Theme {
		DARK, LIGHT, DEFAULT
	}

	static BrowserType browserTypeInitialized = null;

	private static void loadLib() {
		if (!OS.isMacintosh()) {
			if (OS.isWayland()) {
				System.setProperty("chromium.multi_threaded_message_loop", "true");
			}
			multiThreaded = Boolean.getBoolean("chromium.multi_threaded_message_loop");
			if (multiThreaded && Boolean.valueOf(System.getProperty("chromium.debug", "false")))
				debug("J: multi_threaded_message_loop enabled");
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
		String[] args = getChromiumArgs(libsPath, Boolean.getBoolean("chromium.init_threads"), false, null);
		if (!CefApp.startup(args)) {
			throw new RuntimeException("Failed to load binaries for Equo Chromium Browser.");
		}
	}

	private static boolean isCrashReportedEnabled() {
		return System.getProperty("chromium.enable_crash_reporter") == null
				|| Boolean.getBoolean("chromium.enable_crash_reporter");
	}

	static BrowserType getBrowserType() {
		if (Boolean.getBoolean("chromium.force_windowless_swt")) {
			return BrowserType.SWT;
		}
		if (Boolean.getBoolean("chromium.force_windowless_headless")) {
			return BrowserType.HEADLESS;
		}
		try {
			Class<?> clazz = Class.forName("org.eclipse.swt.widgets.Display", false, Engine.class.getClassLoader());
			if (clazz != null) {
				Method findDisplay = clazz.getDeclaredMethod("findDisplay", Thread.class);
				for (Thread thread : Thread.getAllStackTraces().keySet()) {
					if (findDisplay.invoke(null, thread) != null) {
						return BrowserType.SWT;
					}
				}
			}
		} catch (Throwable e) {
		}
		return BrowserType.HEADLESS;
	}

	public static Path findLibsPath() {
		String chromiumPath = System.getProperty("chromium.path", "");
		if (!chromiumPath.isEmpty() && Files.exists(Paths.get(chromiumPath, SUBDIR).toAbsolutePath()))
			return Paths.get(chromiumPath).toAbsolutePath().normalize();
		String windowing = getWindowing();
		String arch = getArch();
		String binariesBsn = BUNDLE_SYMBOLIC_NAME + ".cef." + windowing + "." + Utils.getOS() + "." + arch;
		try {
			Class<?> fragmentClass = Class.forName("com.equo.chromium.ChromiumFragment" + windowing + arch);
			CodeSource codeSource = fragmentClass.getProtectionDomain().getCodeSource();
			if (codeSource != null) {
				URI loc = toURI(codeSource.getLocation());
				Path fragment = Paths.get(loc).toAbsolutePath().normalize();
				try {
					if (Files.isRegularFile(fragment)) {
						Path extractPath = ResourceExpander.extractFromJar(chromiumPath, arch, SUBDIR, loc);
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
							if ((OS.isLinux() || OS.isWindows()) && isHelperExecutable(file)) {
								Path path = createExecutableWithCustomName(file);
								ResourceExpander.setExecutable(path.toFile());
							}
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
	public static URI toURI(URL url) throws URISyntaxException, UnsupportedEncodingException {
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

	private static String removeScheme(String input) {
		if (input == null) {
			return null;
		}
		String regex = "^[a-zA-Z][a-zA-Z0-9+\\-.]*:/+";
		String pathWithoutScheme = input.replaceFirst(regex, "");

		if (!pathWithoutScheme.matches("^[A-Za-z]:.*") && !pathWithoutScheme.startsWith("/")) {
			pathWithoutScheme = "/" + pathWithoutScheme;
		}
		return pathWithoutScheme;
	}

	// Used from test
	private static void setFileSystem(FileSystem fs) {
		fileSystem = fs;
	}

	private static String getChromiumHome() {
		String parameterizedCachePath = System.getProperty("chromium.cache_path");
		String workspace = System.getProperty("osgi.instance.area");
		Path defaultPath = fileSystem.getPath(System.getProperty("user.home"), ".equo");

		try {
			Path chromiumPath;
			String processedWorkspace = null;
			if (workspace != null && !workspace.isEmpty()) {
				processedWorkspace = removeScheme(workspace);
				if (OS.isWindows()) {
					processedWorkspace = processedWorkspace.replaceFirst("^/work/([A-Z]):", "$1:");
				}
			}

			if (parameterizedCachePath != null && !parameterizedCachePath.isEmpty()) {
				Path cacheBase = Paths.get(parameterizedCachePath);
				if (processedWorkspace != null) {
					String workspaceName = Paths.get(processedWorkspace).getFileName().toString();
					chromiumPath = cacheBase.resolve(workspaceName)
						.resolve(".metadata")
						.resolve(".plugins")
						.resolve(BUNDLE_SYMBOLIC_NAME);
				} else {
					chromiumPath = cacheBase.resolve(".metadata")
						.resolve(".plugins")
						.resolve(BUNDLE_SYMBOLIC_NAME);
				}
			} else {
				if (processedWorkspace != null) {
					Path base = fileSystem.getPath(processedWorkspace);
					chromiumPath = fileSystem.getPath(base.toString(), ".metadata", ".plugins", BUNDLE_SYMBOLIC_NAME);
				} else {
					chromiumPath = defaultPath;
				}
			}
			if (!acquireLock(chromiumPath)) {
				chromiumPath = Files.createTempDirectory("equo_session_");
				tempUserDataPath = chromiumPath;
				System.err.println(
						"WARNING: Chromium cache path is in use. Using temporary profile that will be deleted on exit: "
								+ chromiumPath.toAbsolutePath());
			}
			Files.createDirectories(chromiumPath);
			return chromiumPath.toAbsolutePath().toString();
		} catch (IOException e) {
			try {
				Files.createDirectories(defaultPath);
				return defaultPath.toAbsolutePath().toString();
			} catch (IOException fallbackError) {
				System.err.println("ERROR: Could not create default directory: " + defaultPath + "\n" + fallbackError);
				return defaultPath.toAbsolutePath().toString();
			}
		}
	}

	private static boolean acquireLock(Path path) {
		if (path.getFileSystem() != FileSystems.getDefault()) {
			// Cross-process locking is meaningless outside the real OS filesystem
			// (e.g. an injected FileSystem used by tests), so there is nothing to
			// contend for.
			return true;
		}
		try {
			Files.createDirectories(path);
			java.io.File lockFile = path.resolve(".equo_lock").toFile();
			lockRaf = new java.io.RandomAccessFile(lockFile, "rw");
			cacheLock = lockRaf.getChannel().tryLock();
			if (cacheLock == null) {
				lockRaf.close();
				lockRaf = null;
				return false;
			}
			return true;
		} catch (Exception e) {
			try {
				if (lockRaf != null)
					lockRaf.close();
			} catch (IOException ex) {
			}
			lockRaf = null;
			return false;
		}
	}

	public static void initCEF() {
		synchronized (Engine.class) {
			if (!shutdownHookRegistered) {
				shutdownHookRegistered = true;
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					if (cacheLock != null) {
						try {
							cacheLock.release();
						} catch (IOException e) {
							System.err.println("Error releasing lock: " + e.getMessage());
						}
					}
					if (lockRaf != null) {
						try {
							lockRaf.close();
						} catch (IOException e) {
							System.err.println("Error closing file: " + e.getMessage());
						}
					}
					deleteTempUserData();
				}));
			}
		}
		initCEF(BrowserType.SWT);
	}

	static void initCEF(BrowserType browserType) {
		synchronized (Engine.class) {
			if (app == null) {
				browserTypeInitialized = browserType;
				CefSettings settings = new CefSettings();
				try {
					settings.remote_debugging_port = getDebuggingPort();
				} catch (NumberFormatException e) {
				}

				Path data = Paths.get(System.getProperty("chromium.home", getChromiumHome()));
				String cache = data.resolve("cefcache").toAbsolutePath().toString();
				settings.cache_path = cache;
				settings.root_cache_path = cache;
				if (settings.cache_path.isEmpty()) {
					try {
						rootCacheFolder = Files.createTempDirectory("cefTmpCache");
						settings.root_cache_path = rootCacheFolder.toAbsolutePath().toString();
					} catch (IOException e) {
						
					}
				}
				settings.log_file = System.getProperty("chromium.log_file", data.resolve("cef.log").toString());
				settings.log_severity = Boolean.getBoolean("chromium.debug") ? CefSettings.LogSeverity.LOGSEVERITY_INFO
						: CefSettings.LogSeverity.LOGSEVERITY_DISABLE;

				boolean external_message_pump = true;
				if (OS.isMacintosh()) {
					settings.browser_subprocess_path = libsPath
							.resolve(DEFAULT_EXECUTABLE_NAME + ".app/Contents/MacOS/" + CUSTOM_EXECUTABLE_NAME)
							.toString();
					settings.resources_dir_path = libsPath.resolve("Chromium Embedded Framework.framework")
							.resolve("Resources").toString();
				} else if (OS.isWindows()) {
					settings.browser_subprocess_path = libsPath.resolve(CUSTOM_EXECUTABLE_NAME + ".exe").toString();
					settings.resources_dir_path = libsPath.toString();
					settings.locales_dir_path = libsPath.resolve("locales").toString();
				} else if (OS.isLinux()) {
					settings.browser_subprocess_path = libsPath.resolve(CUSTOM_EXECUTABLE_NAME).toString();
					settings.resources_dir_path = libsPath.toString();
					settings.locales_dir_path = libsPath.resolve("locales").toString();
					external_message_pump = false;
				}
				settings.multi_threaded_message_loop = multiThreaded;
				settings.external_message_pump = !settings.multi_threaded_message_loop
						&& (System.getProperty("chromium.external_message_pump") != null
								? Boolean.getBoolean("chromium.external_message_pump")
								: external_message_pump);

				String[] args = getChromiumArgs(libsPath, false, settings.remote_debugging_port > 0, browserType);

				Theme theme = getTheme(Arrays.asList(args));
				if (theme == Theme.DARK || (theme != Theme.LIGHT && BrowserType.SWT.equals(browserType)
						&& SWTEngine.isSystemDarkTheme())) {
					CefColor.setDarkMode(true);
				}

				final SchemeHandlerManager schemeHandlerManager = SchemeHandlerManager.get();

				List<SchemeDomainPair> registeredSchemeData;
				if (schemeHandlerManager != null) {
					registeredSchemeData = schemeHandlerManager.getRegisteredSchemes();
					middlewareResourceRequestHandler = new MiddlewareResourceRequestHandler(schemeHandlerManager,
							registeredSchemeData);
				} else {
					registeredSchemeData = Collections.emptyList();
				}

				WindowingToolkit windowToolkit = null;
				switch (browserType) {
				case HEADLESS:
				case STANDALONE:
					windowToolkit = new CefAppStandalone();
					settings.external_message_pump = OS.isMacintosh() && Boolean.getBoolean("chromium.external_message_pump");
					break;
				case SWING:
					if (OS.isLinux()) {
						System.loadLibrary("jawt");
					}
					windowToolkit = new CefAppSwing();
					settings.external_message_pump = OS.isMacintosh();
					settings.multi_threaded_message_loop = !OS.isMacintosh();
					break;
				default:
					int loopTime = (!settings.external_message_pump && !registeredSchemeData.isEmpty()) ? 1000 / 180
							: WindowingToolkit.DEFAULT_LOOP_TIME;
					windowToolkit = new CefAppSwt(loopTime, settings.external_message_pump);
					break;
				}

				CefApp.setWindowingToolkit(windowToolkit);

				CefApp.addAppHandler(new CefAppHandlerAdapter(args) {
					@Override
					public void onBeforeCommandLineProcessing(String process_type, CefCommandLine command_line) {
						super.onBeforeCommandLineProcessing(process_type, command_line);
						if (Boolean.getBoolean("chromium.disable-frame-restrictions")) {
							command_line.appendSwitch("disable-frame-embedding-restrictions");
						}
					}

					@Override
					public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
						if (!registeredSchemeData.isEmpty()) {
							for (SchemeDomainPair schemeDomain : registeredSchemeData) {
								String scheme = schemeDomain.getScheme();
								registrar.addCustomScheme(scheme, true, false, false, true, true, false, true);
							}
						}
					}

					@SuppressWarnings("unused")
					public boolean enableSecurity() {
						SecurityManager securityManager = SecurityManager.get();
						return securityManager != null && securityManager.isEnabled();
					}

					@Override
					public void onScheduleMessagePumpWork(long delay_ms) {
						if (shuttingDown.get() || (OS.isMacintosh() && browserType == BrowserType.SWING
								&& CefApp.getState() == CefAppState.TERMINATED)) {
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
						if (BrowserType.SWT.equals(browserType)) {
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
				debug("------- Chromium properties: -------");
				printSystemProperties();
				try {
					printVersions(browserType);
				} catch (Throwable e) {
					debug("Error printing properties");
				}
				debug("--------- End Chromium properties ---------");
				if (BrowserType.SWT.equals(browserType)) {
					SWTEngine.initCef(closing, shuttingDown, () -> internalShutdown());
				}
			}
		}
	}

	public static int getDebuggingPort() {
		return Integer
				.parseInt(System.getProperty("chromium.debug_port",
						System.getProperty("chromium.remote_debugging_port", "0")));
	}

	private static void printVersions(BrowserType browserType) {
		Properties chromiumProperties = getPropertiesFromResource(SUBDIR + "/chromium.properties");
		debug("chromium.cef version="
				+ chromiumProperties.getProperty("version", System.getProperty("chromium.cef version", "")));

		Properties versionProperties = getPropertiesFromResource("version.properties");
		debug("cef patch=" + versionProperties.getProperty("cefPatch", ""));
		debug("cef version=" + versionProperties.getProperty("cefVersion", ""));

		Properties manifest = getPropertiesFromResource("META-INF/MANIFEST.MF");
		debug("chromium.swt version=" + manifest.getProperty("Bundle-Version", ""));

		if (BrowserType.SWT.equals(browserType)) {
			debug("SWT version=" + SWTEngine.getSWTVersion());
		}
	}

	public static Properties getPropertiesFromResource(String resourcePath) {
		Properties props = new Properties();

		ClassLoader classLoader = Engine.class.getClassLoader();
		Enumeration<URL> resources = null;
		try {
			resources = classLoader.getResources(resourcePath);
		} catch (IOException e) {
			return props;
		}

		int count = 0;
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			if (count >= 1) {
				CodeSource codeSource = Engine.class.getProtectionDomain().getCodeSource();
				if (codeSource == null || !url.toString().contains(codeSource.getLocation().toString())) {
					continue;
				}
			}
			try (InputStream is = url.openStream()) {
				props.load(is);
			} catch (IOException e) {
				System.err.println("Error getting resource: " + resourcePath);
			}
			count++;
		}
		return props;
	}

	public static String[] getPropertyNames() {
		String[] propertyNames = {
				"chromium.args", //
				"chromium.auto_close_download_dialog", //
				"chromium.cache_path", //
				"chromium.debug", //
				"chromium.debug_port", //
				"chromium.dialogs", //
				"chromium.disable_download_progress", //
				"chromium.disable_close_windowless_before_dispose", //
				"chromium.disable_script_extensions", //
				"chromium.downloadLocationListener", //
				"chromium.enable_crash_reporter", //
				"chromium.external_message_pump", //
				"chromium.find_dialog", //
				"chromium.force_windowless_headless", //
				"chromium.force_windowless_swt", //
				"chromium.headless", //
				"chromium.home", //
				"chromium.inherit_bg_color", //
				"chromium.init_threads", //
				"chromium.log_file", //
				"chromium.manual_stop_browsers", //
				"chromium.multi_threaded_message_loop", //
				"chromium.path", //
				"chromium.proxy_pac_script", //
				"chromium.prevent_swt_shortcuts_forwarding", //
				"chromium.remote_debugging_port", //
				"chromium.resize", //
				"chromium.setTextAsUrl", //
				"chromium.ssl", //
				"chromium.ssl.cert", //
				"chromium.suppress_auth_dialog", //
				"chromium.suspend_threads", //
				"chromium.turbolinks", //
				"chromium.white_bg_color", //
				"java.home", //
				"java.specification.vendor", //
				"java.vendor.version", //
				"java.version", //
				"org.eclipse.swt.internal.deviceZoom", //
				"org.eclipse.swt.internal.gtk.theme", //
				"org.eclipse.swt.internal.gtk.version", //
				"os.arch", //
				"os.name", //
				"os.version", //
				"osgi.ws", //
				"sun.desktop", //
				"user.language" //
			};
		return propertyNames;
	}

	private static void printSystemProperties() {
		for (String propertyName : getPropertyNames()) {
			debug(propertyName + "=" + System.getProperty(propertyName, ""));
		}
		if (OS.isLinux())
			debug("session" + "=" + (OS.isWayland() ? "wayland" : "x11"));
	}

	static String[] getChromiumArgs(Path libsPath, boolean addXInitThreads,
			boolean addRemoteAllowOrigins, BrowserType browserType) {
		List<String> args = new ArrayList<>();
		String vmArg = System.getProperty("chromium.args", System.getProperty("swt.chromium.args"));
		if (vmArg != null) {
			String[] lines = vmArg.replace("\\;", "\\#$").split(";");
			Arrays.stream(lines).map(line -> line.replace("\\#$", ";")).filter(s -> !s.isEmpty())
					.forEach(l -> args.add(l));
		}
		args.add("--disable-chrome-login-prompt");
		if (addRemoteAllowOrigins) {
			args.add("--remote-allow-origins=*");
		}
		if (isCrashReportedEnabled()) {
			args.add("--enable-crash-reporter");
		} else {
			args.add("--disable-crash-reporter");
		}

		Theme theme = getTheme(args);
		if (theme == Theme.DEFAULT && (BrowserType.SWT.equals(browserType) && SWTEngine.isSystemDarkTheme())) {
			args.add("--force-dark-mode");
		} else if (theme == Theme.LIGHT) {
			if (!args.contains("--force-light-mode")) {
				args.add("--force-light-mode");
			}
		}
		if (!OS.isWindows()) {
			if (!Boolean.getBoolean("chromium.enable-chromium-signal-handlers")) {
				args.add("--disable-chromium-signal-handlers");
			}
		}
		if (OS.isLinux()) {
			String gdkScaleStr = System.getenv("GDK_SCALE");
			int _scale = 1;
			if (gdkScaleStr != null) {
				try {
					double scale = Double.parseDouble(gdkScaleStr.replace(',', '.'));
					_scale = (int) Math.floor(scale);
				} catch (NumberFormatException e) {

				}
			} else {
				if (BrowserType.SWT.equals(browserType)) {
					_scale = SWTEngine.getScale();
				}
			}
			if (_scale > 1) {
				args.add("--force-device-scale-factor=" + _scale);
			}
			if (!isWebGLContent(args)) {
				args.add("--disable-gpu-compositing");
			}
			if (addXInitThreads)
				args.add("XInitThreads");
			if (browserType == BrowserType.HEADLESS) {
				args.add("--ozone-platform=headless");
				args.add("--disable-gpu");
			} else if (!checkIfFlagExists(args, "--ozone-platform")) {
				if (OS.isWayland() && browserType != BrowserType.SWING) {
					args.add("--ozone-platform=wayland");
				} else {
					args.add("--ozone-platform=x11");
				}
			}
		} else if (OS.isMacintosh()) {
			args.add("--framework-dir-path=" + libsPath.resolve("Chromium Embedded Framework.framework"));
			args.add("--main-bundle-path=" + libsPath.resolve(DEFAULT_EXECUTABLE_NAME + ".app"));
		} else if (OS.isWindows()) {
			String langFlag = addFlagWithSystemLanguage();
			if (!checkIfFlagExists(args, "--lang") && !langFlag.isEmpty()) {
				args.add(langFlag);
			}
			args.add("--do-not-de-elevate");
		}
		return args.toArray(new String[args.size()]);
	}

	static boolean isWebGLContent(List<String> args) {
		return checkIfFlagExists(args, "--enable-webgl") || checkIfFlagExists(args, "--enable-webgl2")
				|| checkIfFlagExists(args, "--enable-webgl-draft-extensions");
	}

	private static void internalShutdown() {
		if (app == null) {
			return;
		}

		if (OS.isLinux()) {
			final CefApp cefApp = app;
			CompletableFuture.runAsync(() -> {
				cefApp.dispose();
				deleteTempFolder();
			});
		} else {
			app.dispose();
			deleteTempFolder();
		}
		app = null;
	}

	public static <T extends CefClient> T createClient() {
		T client = app.createClient();
		if (CefAppState.INITIALIZATION_FAILED == CefApp.getState()) {
			throw new IllegalStateException("Failed to initialize Chromium. chromium.cache_path is already in use by another process.");
		}
		return client;
	}

	public static void startCefLoop() {
		app.runMessageLoop();
	}

	public static void quitCefLoop() {
		app.quitMessageLoop();
	}

	private static String getArch() {
		String osArch = System.getProperty("os.arch");
		if (osArch.equals("i386") || osArch.equals("i686"))
			return "x86";
		if (osArch.equals("amd64"))
			return "x86_64";
		return osArch;
	}

	private static Theme getTheme(List<String> args) {
		if (checkIfFlagExists(args, "--ignore-dark-mode") || checkIfFlagExists(args, "--force-light-mode")) {
			return Theme.LIGHT;
		}
		if (checkIfFlagExists(args, "--force-dark-mode")) {
			return Theme.DARK;
		}
		return Theme.DEFAULT;
	}

	private static boolean checkIfFlagExists(List<String> args, String flag) {
		for (String str : args) {
			if (str.contains(flag)) {
				return true;
			}
		}
		return false;
	}

	private static String addFlagWithSystemLanguage() {
		ArrayList<String> suportedLanguages = new ArrayList<String>();
		ArrayList<String> suportedLanguagesWithCountry = new ArrayList<String>();
		Collections.addAll(suportedLanguages, "af", "am", "ar", "bg", "bn", "ca", "cs", "da", "de", "el", "es", "et",
				"fa", "fi", "fil", "fr", "gu", "he", "hi", "hr", "hu", "id", "it", "ja", "kn", "ko", "lt", "lt", "ml",
				"mr", "ms", "nb", "nl", "pl", "ro", "ru", "sk", "sl", "sr", "sv", "sw", "ta", "te", "th", "tr", "uk",
				"ur", "vi");
		Collections.addAll(suportedLanguagesWithCountry, "en-GB", "en-US", "es-419", "pt-BR", "pt-PT", "zh-CN",
				"zh-TW");
		Locale currentLocale = Locale.getDefault();
		if (suportedLanguages.contains(currentLocale.getLanguage())) {
			return "--lang=" + currentLocale.getLanguage();
		} else if (suportedLanguagesWithCountry
				.contains(currentLocale.getLanguage() + "-" + currentLocale.getCountry())) {
			return "--lang=" + currentLocale.getLanguage() + "-" + currentLocale.getCountry();
		} else {
			return "";
		}
	}

	private static void deleteTempFolder() {
		if (rootCacheFolder == null || !Files.exists(rootCacheFolder)) {
			return;
		}
		try {
			Files.walk(rootCacheFolder).sorted(Comparator.reverseOrder()).forEach(t -> {
				try {
					if (Files.isRegularFile(t)) {
						if (!t.toString().toLowerCase().endsWith(".dmp")) {
							Files.deleteIfExists(t);
						}
					} else if (Files.isDirectory(t)) {
						Files.deleteIfExists(t);
					}
				} catch (IOException e) {

				}
			});
		} catch (IOException e) {

		}
	}

	private static void deleteTempUserData() {
		if (tempUserDataPath != null) {
			try (java.util.stream.Stream<Path> walk = Files.walk(tempUserDataPath)) {
				walk.sorted(Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(java.io.File::delete);
			} catch (IOException e) {
			} catch (java.io.UncheckedIOException e) {
			}
		}
	}
}
