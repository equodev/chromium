package com.make.cef.samples.cefsimple;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cef.capi.CEF;
import cef.capi.CEF.App;
import cef.capi.CEF.BrowserProcessHandler;
import cef.capi.CEF.Client;
import cef.capi.CEF.CommandLine;
import cef.capi.CEF.LogSeverity;
import cef.capi.CEF.MainArgs;
import cef.capi.CEF.ProcessId;
import cef.capi.CEF.Settings;
import cef.capi.CEF.StringUtf16;
import jnr.ffi.Memory;
import jnr.ffi.Pointer;
import jnr.ffi.Struct;
import jnr.ffi.provider.ParameterFlags;
import jnr.posix.POSIXFactory;
import x11.xlib.X11;
import x11.xlib.X11.XLib;

public class CefJNR {
/*	static {
		System.setProperty("java.system.class.loader", "com.make.cef.samples.cefsimple.AvoidThreadClassLoader");
	}*/
	
	static jnr.ffi.Runtime runtime = CEF.RUNTIME;
//	public static interface Cef {
//		int cef_version_info(int entry);
//		
//		int CefExecuteProcess(Pointer args, Pointer application, Pointer windows_sandbox_info);
//		
//		
//	}
	
	public static void main(String[] args) throws IOException {
//		ClassLoader cl = ClassLoader.getSystemClassLoader();
//		if (cl.getClass() != AvoidThreadClassLoader.class) {
//			throw new RuntimeException(cl.toString());
//		}
		
//		Cef cef = LibraryLoader.create(Cef.class).load("cef");
//		Cef cef = LibraryLoader.create(Cef.class).load("cef");
//		System.out.println(cef);
//		
////		System.out.println(cef.cef_version_info(2));
//		System.out.println(cef.cef_version_info(4));
//		System.out.println(cef.cef_version_info(5));
		
//		Pointer mainArgs = cef.CefMainArgs(args.length, null);
//		System.out.println(mainArgs);
		
//		Pointer<CefMainArgs> main_args = Pointer.allocate(CefMainArgs.class);
		
        MainArgs main_args = createMainArgs(args, runtime);

		// SimpleApp implements application-level callbacks for the browser process.
		// It will create the first browser instance in OnContextInitialized() after
		// Cef has initialized.
		App app = createApp();

		System.out.println("1ACTIVE THREADS: " + Thread.activeCount());
//		System.out.println("Calling executeProcess");
//		int exit_code = CEF.executeProcess(main_args, null, null);
//		if (exit_code > 0) {
//			// The sub-process has completed so return here.
//			throw new RuntimeException(exit_code + "");
//		}
		
		XLib xlib = X11.getLib();
		xlib.XSetErrorHandler((d, e) -> {
			System.err.println("XSetErrorHandler received");
			return 0;
		});
		xlib.XSetIOErrorHandler((d) -> {
			System.err.println("XSetIOErrorHandler received");
			return 0;
		});

		  // Install xlib error handlers so that the application won't be terminated
		  // on non-fatal errors.
//		  XSetErrorHandler(XErrorHandlerImpl);
//		  XSetIOErrorHandler(XIOErrorHandlerImpl);
		
		Settings settings = createSettings(runtime);

		System.out.println("Calling initialize");
		// Initialize Cef for the browser process.
		int sucess = CEF.initialize(main_args, settings, app, null);
		if (sucess != 1)
			throw new RuntimeException("initialize failed: " +sucess);

//		createBrowser();
		
		System.out.println("Calling runMessageLoop");
		// Run the Cef message loop. This will block until CefQuitMessageLoop() is
		// called.
		CEF.runMessageLoop();

		System.out.println("Calling shutdown");
		// Shut down Cef.
		CEF.shutdown();
		System.out.println("Done");
	}

	public static Settings createSettings(jnr.ffi.Runtime runtime) {
		System.out.println("2ACTIVE THREADS: " + Thread.activeCount());
		// Specify Cef global settings here.
//		String subprocessPath = prepareLauncher();
		String subprocessPath =  System.getProperty("user.dir") + File.separator + "cefgo";
		System.out.println(subprocessPath);
		
		Settings settings = new CEF.Settings(runtime);
		settings.size.set(Struct.size(settings));
		String cef = "cef_binary_3.2883.1539.gd7f087e_linux64";
		settings.resourcesDirPath.set(System.getProperty("user.home") + File.separator + "Downloads/"+cef +"/Resources");
		settings.localesDirPath.set(System.getProperty("user.home") + File.separator + "Downloads/"+cef+"/Resources/locales");
		settings.browserSubprocessPath.set(subprocessPath);
		settings.logFile.set(System.getProperty("user.dir") + File.separator + "ceflog.log" );
		settings.logSeverity.set(LogSeverity.LOGSEVERITY_VERBOSE);
		settings.noSandbox.set(1);
//		settings.command_line_args_disabled.set(0);
//		settings.singleProcess.set(1);
		return settings;
	}

	public static App createApp() {
		BrowserProcessHandler browserProcessHandler = new CEF.BrowserProcessHandler(runtime);
		browserProcessHandler.setOnContextInitialized(new CEF.BrowserProcessHandler.OnContextInitialized() {
			@Override
			public void invoke(Pointer self) {
				System.out.println("- onContextInitialized");
				System.out.println("3ACTIVE THREADS: " + Thread.activeCount());
				createBrowser();
			}
		});
//		browserProcessHandler.base.size.set(Struct.size(browserProcessHandler));

		App app = new App(runtime);
//		app.setOnBeforeCommandLineProcessing((Pointer self, Pointer stringUtf16, Pointer commandLine) -> {
//			DEBUG_CALLBACK("- onBeforeCommandLineProcessing");
//			CommandLine cmd = new CEF.CommandLine(runtime);
//			cmd.useMemory(commandLine);
//			cmd.isReadOnly.
//		});
//		app.setOnRegisterCustomSchemes((app1, schemeRegistrar_1) -> DEBUG_CALLBACK("on_register_custom_schemes"));
//		app.setGetResourceBundleHandler(app1 -> /*DEBUG_CALLBACK("get_resource_bundle_handler")*/null);
		app.setGetBrowserProcessHandler(app1 -> {
			System.out.println("- getBrowserProcessHandler");
			System.out.println("4ACTIVE THREADS: " + Thread.activeCount());
			return browserProcessHandler;
		});
//		app.setGetRenderProcessHandler(app1 -> DEBUG_CALLBACK("get_render_process_handler"));
		return app;
	}

	public static void createBrowser() {
		// Create GTK window. You can pass a NULL handle 
		// to CEF and then it will create a window of its own.
//			    initialize_gtk();
//			    GtkWidget* hwnd = create_gtk_window("cefcapi example", 1024, 768);
		CEF.WindowInfo windowInfo = new CEF.WindowInfo(runtime);
//			    windowInfo.parent_widget = hwnd;

		// Browser settings.
		// It is mandatory to set the "size" member.
		CEF.BrowserSettings browserSettings = new CEF.BrowserSettings(runtime);
		browserSettings.size.set(Struct.size(browserSettings));
		// Client handler and its callbacks.
		// cef_client_t structure must be filled. It must implement
		// reference counting. You cannot pass a structure 
		// initialized with zeroes.
		Client client = new CEF.Client(runtime);
		initializeClientHandler(client);

		StringUtf16 url = new CEF.StringUtf16(runtime);
		url.set("http://google.com");
		// Create browser.
		System.out.println("Calling cef_browser_host_create_browser");
		if (CEF.browserHostCreateBrowser(windowInfo, client, url, browserSettings, null) != 1)
			throw new RuntimeException("Failed calling browserHostCreateBrowser");
	}

	protected static void initializeClientHandler(Client client) {
	    System.out.println("initialize_client_handler");
	    // callbacks
	    client.setGetContextMenuHandler((c) -> DEBUG_CALLBACK("get_context_menu_handler"));
	    client.setGetDialogHandler((c) -> DEBUG_CALLBACK("get_dialog_handler"));
	    client.setGetDisplayHandler((c) -> DEBUG_CALLBACK("get_display_handler"));
	    client.setGetDownloadHandler((c) -> DEBUG_CALLBACK("get_download_handler"));
	    client.setGetDragHandler((c) -> DEBUG_CALLBACK("get_drag_handler"));
	    client.setGetFocusHandler((c) -> DEBUG_CALLBACK("get_focus_handler"));
	    client.setGetGeolocationHandler((c) -> DEBUG_CALLBACK("get_geolocation_handler"));
	    client.setGetJsdialogHandler((c) -> DEBUG_CALLBACK("get_jsdialog_handler"));
	    client.setGetKeyboardHandler((c) -> DEBUG_CALLBACK("get_keyboard_handler"));
	    client.setGetLifeSpanHandler((c) -> DEBUG_CALLBACK("get_life_span_handler"));
	    client.setGetLoadHandler((c) -> DEBUG_CALLBACK("get_load_handler"));
	    client.setGetRenderHandler((c) -> DEBUG_CALLBACK("get_render_handler"));
	    client.setGetRequestHandler((c) -> DEBUG_CALLBACK("get_request_handler"));
	    client.setOnProcessMessageReceived((c, browser_1, processId_2, processMessage_3) -> {
	    	DEBUG_CALLBACK("on_process_message_received"); 
	    	return 0;
	    });
	}

	public static MainArgs createMainArgs(String[] args, jnr.ffi.Runtime runtime) {
        System.out.println("Creating args");

		List<String> argsList = new ArrayList<>(Arrays.asList(args));
//		argsList.add(0, "cefswt");
//		argsList.add("--disable-namespace-sandbox");
//		argsList.add("--disable-gpu");
		System.out.println(argsList);
		
		MainArgs main_args = new MainArgs(runtime);
		main_args.argc.set(argsList.size());

		Pointer[] array = new Pointer[argsList.size()];
        for (int i = 0; i < array.length; i++) {
        	String argv = argsList.get(i);
			array[i] = Memory.allocateDirect(runtime, argv.length());
            array[i].putString(0, argv, argv.length(), Charset.defaultCharset());
        }

		jnr.ffi.Pointer stringp = Memory.allocateDirect(runtime, array.length * (runtime.addressSize()));
		stringp.put(0, array, 0, array.length);
		main_args.argv.set(stringp);
		return main_args;
	}

	private static String prepareLauncher() {
		int pid = POSIXFactory.getPOSIX().getpid();
		
		File file = new File("subprocess"+pid+".sh");
		if (!file.exists()) {
	//		String cwd = System.getProperty("user.dir");
			String ld = System.getProperty("java.library.path");
			String cp = System.getProperty("java.class.path");
			String main = System.getProperty("sun.java.command");
			String javaHome = System.getProperty("java.home");
			
			StringBuilder buffer = new StringBuilder("#! /bin/bash\n")
				.append("CEF_ARGS=$@\n")
				.append("echo \"LAUNCHING JAVA\"\n")
				//.append("echo $CEF_ARGS\n")
				.append(javaHome).append("/bin/java")
	//			.append("java")
				.append(" -cp ").append(cp)
				.append(" -Djava.library.path=").append(ld)
				.append(" ").append(main).append(" ").append("$CEF_ARGS")
				.append("\necho END")
				;
			
			
	//		System.out.println(buffer.toString());
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				writer.write(buffer.toString());
				file.setExecutable(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return file.getAbsolutePath();
	}

	private static jnr.ffi.Pointer DEBUG_CALLBACK(String log) {
		System.out.println(log);
		return null;
	}
}
