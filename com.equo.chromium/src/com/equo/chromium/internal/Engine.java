package com.equo.chromium.internal;

import java.io.File;
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
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cef.CefApp;
import org.cef.CefAppStandalone;
import org.cef.CefAppSwing;
import org.cef.CefAppSwt;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.SystemBootstrap;
import org.cef.SystemBootstrap.Loader;
import org.cef.WindowingToolkit;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserSwt;
import org.cef.callback.CefCommandLine;
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

	public static boolean setDarkModeBackground = false;

	public static enum BrowserType {
		SWT, STANDALONE, SWING, HEADLESS
	}

	static BrowserType browserTypeInitialized = null;

	private static void loadLib() {
		if (OS.isLinux()) {
			multiThreaded = Boolean.getBoolean("chromium.multi_threaded_message_loop");
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
		String[] args = getChromiumArgs(libsPath, Boolean.getBoolean("chromium.init_threads"), checkGtkInit, null);
		setupCrashReporter();
		if (!CefApp.startup(args)) {
			if (checkGtkInit) {
				throw new RuntimeException("To run Chromium on Wayland, set env var GDK_BACKEND=x11 or call ChromiumBrowser.earlyInit() before creating a window");
			}
			throw new RuntimeException("Failed to load binaries for Equo Chromium Browser.");
		}
	}

	private static void setupCrashReporter() {
		File crashReportConfig = Paths.get(libsPath.toString(), "crash_reporter.cfg").toFile();
		File crashReportDisabledConfig = Paths.get(libsPath.toString(), "crash_reporter_disabled.cfg").toFile();
		if (isCrashReportedEnabled()) {
			if (!crashReportConfig.exists() && crashReportDisabledConfig.exists()) {
				crashReportDisabledConfig.renameTo(crashReportConfig);
			}
		} else {
			if (crashReportConfig.exists()) {
				crashReportConfig.renameTo(crashReportDisabledConfig);
			}
		}
	}

	private static boolean isCrashReportedEnabled() {
		return System.getProperty("chromium.enable_crash_reporter") == null
				|| Boolean.getBoolean("chromium.enable_crash_reporter");
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
		initCEF(BrowserType.SWT);
	}

	static void initCEF(BrowserType browserType) {
		synchronized (Engine.class) {
			if (app == null) {
				browserTypeInitialized = browserType;
				CefSettings settings = new CefSettings();
				try {
					settings.remote_debugging_port = Integer
							.parseInt(System.getProperty("chromium.debug_port",
								System.getProperty("chromium.remote_debugging_port", "0")));
				} catch (NumberFormatException e) {
				}
				Path data = Paths.get(System.getProperty("chromium.home", System.getProperty("user.home")), ".equo");
				String cache = data.resolve("cefcache").toAbsolutePath().toString();
				settings.cache_path = System.getProperty("chromium.cache_path", cache);
				settings.log_file = System.getProperty("chromium.log_file", data.resolve("cef.log").toString());
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

				String[] args = getChromiumArgs(libsPath, false, false, browserType);

				if (isDarkTheme(browserType, Arrays.asList(args))) {
					CefBrowserSwt.setDarkMode(true);
				}

				final SchemeHandlerManager schemeHandlerManager = SchemeHandlerManager.get();

				if (schemeHandlerManager != null) {
					registeredSchemeData = schemeHandlerManager.getRegisteredSchemes();
				} else {
					registeredSchemeData = Collections.emptyList();
				}

				WindowingToolkit windowToolkit = null;
				switch (browserType) {
				case HEADLESS:
				case STANDALONE:
					windowToolkit = new CefAppStandalone();
					settings.external_message_pump = false;
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
					int loopTime = (!settings.external_message_pump && !registeredSchemeData.isEmpty()) ? 1000 / 180 : WindowingToolkit.DEFAULT_LOOP_TIME;
					windowToolkit = new CefAppSwt(loopTime, settings.external_message_pump);
					break;
				}

				CefApp.setWindowingToolkit(windowToolkit);

				CefApp.addAppHandler(new CefAppHandlerAdapter(args) {
					@Override
					public void onBeforeCommandLineProcessing(String process_type, CefCommandLine command_line) {
						super.onBeforeCommandLineProcessing(process_type, command_line);
						if (settings.chrome_runtime) {
							command_line.appendSwitchWithValue("browser-subprocess-path",
									settings.browser_subprocess_path);
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
				if (debug) {
					printSystemProperties();
				}
				if (BrowserType.SWT.equals(browserType)) {
					SWTEngine.initCef(closing, shuttingDown, () -> internalShutdown());
				}
			}
		}
	}

	private static void printSystemProperties() {
		System.out.println("------- Chromium system properties: -------");

		String[] propertyNames = { "chromium.args", "chromium.cache_path", "chromium.custom_protocol", "chromium.debug",
				"chromium.debug_port", "chromium.dialogs", "chromium.disable-download-progress",
				"chromium.disable_script_extensions", "chromium.downloadLocationListener",
				"chromium.enable_crash_reporter", "chromium.external_message_pump", "chromium.find_dialog",
				"chromium.force_windowless_swt", "chromium.headless", "chromium.home", "chromium.init_threads",
				"chromium.log_file", "chromium.multi_threaded_message_loop", "chromium.path",
				"chromium.proxy_pac_script", "chromium.remote_debugging_port", "chromium.resize",
				"chromium.setTextAsUrl", "chromium.ssl", "chromium.ssl.cert", "chromium.suspend_threads",
				"chromium.turbolinks", "java.home", "java.specification.vendor", "java.vendor.version", "java.version",
				"org.eclipse.swt.internal.deviceZoom", "org.eclipse.swt.internal.gtk.theme",
				"org.eclipse.swt.internal.gtk.version", "os.arch", "os.name", "os.version", "osgi.ws", "sun.desktop",
				"user.language" };

		for (String propertyName : propertyNames) {
			System.out.println(propertyName + "=" + System.getProperty(propertyName, ""));
		}
		System.out.println("--------- End Chromium properties ---------");
	}

	static String[] getChromiumArgs(Path libsPath, boolean addXInitThreads, boolean addGtkInitCheck, BrowserType browserType) {
		List<String> args = new ArrayList<>();
		String vmArg = System.getProperty("chromium.args", System.getProperty("swt.chromium.args"));
		if (vmArg != null) {
			String[] lines = vmArg.replace("\\;", "\\#$").split(";");
			Arrays.stream(lines).map(line -> line.replace("\\#$", ";")).forEach(l -> args.add(l));
		}
		if (isCrashReportedEnabled()) {
			args.add("--enable-crash-reporter");
		}
		if (OS.isLinux()) {
			args.add("--disable-gpu-compositing");
			if (addXInitThreads)
				args.add("XInitThreads");
			if (addGtkInitCheck)
				args.add("GTKInitCheck");
			if (isDarkTheme(browserType, args)) {
				args.add("--force-dark-mode");
			}
			if (browserType == BrowserType.HEADLESS) {
				args.add("--ozone-platform=headless");
				args.add("--disable-gpu");
			}
		} else if (OS.isMacintosh()) {
			args.add("--framework-dir-path=" + libsPath.resolve("Chromium Embedded Framework.framework"));
			args.add("--main-bundle-path=" + libsPath.resolve("equochro Helper.app"));
			if (isDarkTheme(browserType, args)) {
				args.add("--force-dark-mode");
			}
		} else if (OS.isWindows()) {
			String langFlag = addFlagWithSystemLanguage();
			if(!checkIfFlagExists(args, "--lang") && !langFlag.isEmpty()) {
				args.add(langFlag);
			}
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
	
	private static boolean isDarkTheme(BrowserType browserType, List<String> args) {
		if (browserType != null && BrowserType.SWT.equals(browserType)) {
			boolean ignoreDarkMode = checkIfFlagExists(args, "--ignore-dark-mode");
			boolean forceDarkMode = checkIfFlagExists(args, "--force-dark-mode");
			setDarkModeBackground = ignoreDarkMode ? false : forceDarkMode ? true : SWTEngine.isSystemDarkTheme();
			return setDarkModeBackground;
		}
		return false;
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
		Collections.addAll(suportedLanguages, "af", "am", "ar", "bg", "bn", "ca", "cs", "da", "de", "el", "es", "et", "fa", "fi", "fil", "fr", "gu", "he", "hi", "hr", "hu", "id", "it", "ja", "kn", "ko", "lt", "lt", "ml", "mr", "ms", "nb", "nl", "pl", "ro", "ru", "sk", "sl", "sr", "sv", "sw", "ta", "te", "th", "tr", "uk", "ur", "vi");
		Collections.addAll(suportedLanguagesWithCountry, "en-GB", "en-US", "es-419", "pt-BR", "pt-PT", "zh-CN", "zh-TW");
		Locale currentLocale = Locale.getDefault();
		if (suportedLanguages.contains(currentLocale.getLanguage())) {
			return "--lang=" + currentLocale.getLanguage();
		} else if (suportedLanguagesWithCountry.contains(currentLocale.getLanguage() + "-" + currentLocale.getCountry())) {
			return "--lang=" + currentLocale.getLanguage() + "-" + currentLocale.getCountry();
		} else {			
			return "";
		}
	}
}
