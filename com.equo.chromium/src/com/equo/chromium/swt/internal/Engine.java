package com.equo.chromium.swt.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.cef.CefApp;
import org.cef.CefAppSwt;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.SystemBootstrap;
import org.cef.SystemBootstrap.Loader;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefAppHandlerAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

class Engine {
	static {
		loadLib();
	}
	private static final String CEFVERSION = "3987";
	private static final String SUBDIR = "chromium-" + CEFVERSION;
	private static final String SCHEME_FILE = "file"; //$NON-NLS-1$
	private static Path libsPath;

	static final boolean debug = Boolean.valueOf(System.getProperty("chromium.debug", "false"));
	
	private static boolean shuttindDown;

	private static CefApp app;
	static CompletableFuture<Boolean> ready = new CompletableFuture<>();
	private static boolean closing;
	
	private static void loadLib() {
		if (OS.isLinux()) {
			String backend = System.getenv("GDK_BACKEND");
			String session = System.getenv("XDG_SESSION_TYPE");
			if ("wayland".equals(session) && !"x11".equals(backend)) {
				throw new SWTException(SWT.ERROR_FAILED_LOAD_LIBRARY, "To run Chromium on Wayland, set env var GDK_BACKEND=x11");
			}
		}
		
		libsPath = findLibsPath().resolve(SUBDIR);
		SystemBootstrap.setLoader(new Loader() {
			@Override
			public void loadLibrary(String libname) {
				System.load(libsPath.resolve(System.mapLibraryName(libname)).toString());
			}
		});
		String[] args = getChromiumArgs(libsPath);
		if (!Files.exists(libsPath) || !CefApp.startup(args)) {
			throw new SWTException(SWT.ERROR_FAILED_LOAD_LIBRARY, "Missing binaries for Equo Chromium Browser.");
		}
	}

	private static Path findLibsPath() {
		String chromiumPath = System.getProperty("chromium.path", "");
		if (!chromiumPath.isEmpty() && Files.exists(Paths.get(chromiumPath, SUBDIR).toAbsolutePath()))
			return Paths.get(chromiumPath).toAbsolutePath().normalize();
		String arch = getArch();
		String binariesBsn = "com.equo.chromium.cef."+SWT.getPlatform()+"."+getOS()+"."+arch;
		try {
			Class<?> fragmentClass = Class.forName("com.equo.chromium.ChromiumFragment");
			CodeSource codeSource = fragmentClass.getProtectionDomain().getCodeSource();
			if (codeSource != null) {
				URI loc = toURI(codeSource.getLocation());
				Path fragment = Paths.get(loc);
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
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		SWTException swtError = new SWTException(SWT.ERROR_FAILED_LOAD_LIBRARY, "plugin/jar '"+binariesBsn+"' is missing and system property 'chromium.path' is not correctly set.");
		throw swtError;
	}

	/**
	 * Returns the URL as a URI. This method will handle URLs that are
	 * not properly encoded (for example they contain unencoded space characters).
	 * 
	 * @param url The URL to convert into a URI
	 * @return A URI representing the given URL
	 */
	private static URI toURI(URL url) throws URISyntaxException {
		//URL behaves differently across platforms so for file: URLs we parse from string form
		if (SCHEME_FILE.equals(url.getProtocol())) {
			String pathString = url.toExternalForm().substring(5);
			//ensure there is a leading slash to handle common malformed URLs such as file:c:/tmp
			if (pathString.indexOf('/') != 0)
				pathString = '/' + pathString;
			return new URI(SCHEME_FILE, null, pathString, null);
		}
		try {
			return new URI(url.toExternalForm());
		} catch (URISyntaxException e) {
			//try multi-argument URI constructor to perform encoding
			return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		}
	}
	
	static void initCEF() {
		synchronized (Engine.class) {
			if (app == null) {
				CefSettings settings = new CefSettings();
				try {
					settings.remote_debugging_port = Integer.parseInt(System.getProperty("chromium.remote_debugging_port", "0"));
				} catch (NumberFormatException e) {
				}
				Path data = Paths.get(System.getProperty("user.home"), ".equo");
				String cache = data.resolve("cefcache").toAbsolutePath().toString();
				settings.cache_path = System.getProperty("chromium.cache_path", cache);
				settings.log_file = data.resolve("cef.log").toString();
				settings.log_severity = debug ? CefSettings.LogSeverity.LOGSEVERITY_INFO : CefSettings.LogSeverity.LOGSEVERITY_DISABLE;

				boolean external_message_pump = true;
				if (OS.isMacintosh()) {
					settings.browser_subprocess_path = libsPath.resolve("jcef Helper.app/Contents/MacOS/jcef Helper").toString();
					settings.resources_dir_path = libsPath.resolve("Chromium Embedded Framework.framework").resolve("Resources").toString();
					external_message_pump = false;
				} else if (OS.isWindows()) {
					settings.browser_subprocess_path = libsPath.resolve("jcef_helper.exe").toString();
					settings.resources_dir_path = libsPath.toString();
					settings.locales_dir_path = libsPath.resolve("locales").toString();
				} else if (OS.isLinux()) {
					settings.browser_subprocess_path = libsPath.resolve("jcef_helper").toString();
					settings.resources_dir_path = libsPath.toString();
					settings.locales_dir_path = libsPath.resolve("locales").toString();
					external_message_pump = false;
				}
				settings.external_message_pump = System.getProperty("chromium.external_message_pump") != null ? Boolean.getBoolean("chromium.external_message_pump") : external_message_pump;

				CefAppSwt appSwt = new CefAppSwt(settings.external_message_pump);
				CefApp.setWindowingToolkit(appSwt);
				
				String[] args = getChromiumArgs(libsPath);
				CefApp.addAppHandler(new CefAppHandlerAdapter(args) {
					@Override
					public void onScheduleMessagePumpWork(long delay_ms) {
						if (shuttindDown) {
							debug("Ignoring onScheduleMessagePumpWork due shuttindDown");
							return;
						}
						super.onScheduleMessagePumpWork(delay_ms);
					}
					
					@Override
					public void onContextInitialized() {
						ready.complete(true);
					}
					
					@Override
					public boolean onBeforeTerminate() {
						if (shuttindDown) {
							// already shutdown
							return false;
						}
						shuttindDown = true;
						internalShutdown();
						return true;
					}
				});
				try {
					app = CefApp.getInstance(settings);
				} catch (UnsatisfiedLinkError e) {
					if ("gtk".equals(SWT.getPlatform()) && e.getMessage().contains("libgconf")) {
						System.load(libsPath.resolve("libgconf-2.so.4").toString());
						app = CefApp.getInstance(settings);
					} else {
						throw e;
					}
				}
				Display.getDefault().addListener(SWT.Close, e -> {
					closing = true;
				});
				Display.getDefault().disposeExec(() -> {
					if (shuttindDown) {
						// already shutdown
						return;
					}
					if (!OS.isMacintosh() || !closing) {
						shuttindDown = true;
						internalShutdown();
					}
				});
			}
		}
	}
	
	static String[] getChromiumArgs(Path libsPath) {
		List<String> args = new ArrayList<>();
		String vmArg = System.getProperty("chromium.args", System.getProperty("swt.chromium.args"));
		if (vmArg != null) {
			String[] lines = vmArg.split(";");
			args.addAll(Arrays.asList(lines));
		}
		if (OS.isLinux()) {
			args.add("--disable-gpu-compositing");
		} else if (OS.isMacintosh()) {
			args.add("--framework-dir-path="+libsPath.resolve("Chromium Embedded Framework.framework"));
			args.add("--main-bundle-path="+libsPath.resolve("jcef Helper.app"));
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

	static <T extends CefClient> T createClient() {
		return app.createClient();
	}

	private static String getOS() {
		String os = "";
		switch (SWT.getPlatform()) {
		case "gtk":
			os = "linux";
			break;
		case "win32":
			os = "win32";
			break;
		case "cocoa":
			os = "macosx";
			break;
		}
		return os;
	}
	
	private static String getArch() {
		String osArch = System.getProperty("os.arch");
		if (osArch.equals ("i386") || osArch.equals ("i686")) return "x86";
		if (osArch.equals ("amd64")) return "x86_64";
		return osArch;
	}

	static void debug(String log) {
		if (debug) {
			System.out.println("J:" + log);
		}
	}
	
	static void debug(String log, CefBrowser cefBrowser) {
		if (debug) {
			int identifier = cefBrowser != null ? cefBrowser.getIdentifier() : -1;
			System.out.println(System.currentTimeMillis()/1000+":J"+identifier + ":" + Thread.currentThread().getName() +":" + log);
		}
	}

}
