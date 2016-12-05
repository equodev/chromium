package com.make.cef.samples.cefsimple;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import cef.capi.CEF;
import cef.capi.CEF.App;
import cef.capi.CEF.BrowserProcessHandler;
import cef.capi.CEF.CommandLine;
import cef.capi.CEF.LogSeverity;
import cef.capi.CEF.MainArgs;
import cef.capi.CEF.Settings;
import cef.capi.CEF.StringUtf16;
import jnr.ffi.Memory;
import jnr.ffi.Pointer;
import jnr.ffi.Struct;

public class CefJNR {
	static jnr.ffi.Runtime runtime = CEF.RUNTIME;
//	public static interface Cef {
//		int cef_version_info(int entry);
//		
//		int CefExecuteProcess(Pointer args, Pointer application, Pointer windows_sandbox_info);
//		
//		
//	}
	
	public static void main(String[] args) throws IOException {
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
		
        System.out.println("Creating args");
        System.out.println(Arrays.toString(args));

        MainArgs main_args = createMainArgs(args, runtime);

		// SimpleApp implements application-level callbacks for the browser process.
		// It will create the first browser instance in OnContextInitialized() after
		// Cef has initialized.
		App app = createApp();

		System.out.println("Calling executeProcess");
		int exit_code = CEF.executeProcess(main_args, app, null);
		if (exit_code >= 0) {
			// The sub-process has completed so return here.
			throw new RuntimeException(exit_code + "");
		}

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
		// Specify Cef global settings here.
		String subprocessPath = prepareLauncher();
		
		Settings settings = new CEF.Settings(runtime);
		settings.size.set(Struct.size(settings));
		settings.resourcesDirPath.set("/home/guille/Downloads/cef_binary_3.2704.1434.gec3e9ed_linux64/Resources");
		settings.localesDirPath.set("/home/guille/Downloads/cef_binary_3.2704.1434.gec3e9ed_linux64/Resources/locales");
		settings.browserSubprocessPath.set(subprocessPath);
		settings.logFile.set(System.getProperty("user.dir") + File.separator + "ceflog.log" );
		settings.logSeverity.set(LogSeverity.LOGSEVERITY_VERBOSE);
		settings.noSandbox.set(1);
//		settings.command_line_args_disabled.set(0);
//		settings.single_process.set(0);
		return settings;
	}

	public static App createApp() {
		App app = new App(runtime);
		
		BrowserProcessHandler browserProcessHandler = new CEF.BrowserProcessHandler(runtime);
		browserProcessHandler.setOnContextInitialized(new CEF.BrowserProcessHandler.OnContextInitialized() {
			@Override
			public void invoke(Pointer self) {
				System.out.println("- onContextInitialized");
				
				// Browser settings.
				// It is mandatory to set the "size" member.
				CEF.BrowserSettings browserSettings = new CEF.BrowserSettings(runtime);
			    browserSettings.size.set(Struct.size(browserSettings));
			    
			    // Client handler and its callbacks.
			    // cef_client_t structure must be filled. It must implement
			    // reference counting. You cannot pass a structure 
			    // initialized with zeroes.
//			    cef_client_t client = {};
//			    initialize_client_handler(&client);

			    // Create browser.
			    System.out.println("cef_browser_host_create_browser\n");
//			    cef_browser_host_create_browser(&windowInfo, &client, &cefUrl,
//			            &browserSettings, NULL);
			}
		});
		app.setOnBeforeCommandLineProcessing(new CEF.App.OnBeforeCommandLineProcessing() {
//			@Override
//			public void invoke(Pointer app, jnr.ffi.Pointer process_type, jnr.ffi.Pointer command_line) {
//				System.out.println("- onBeforeCommandLineProcessing");
//			}

			@Override
			public void invoke(Pointer app, StringUtf16 stringUtf16, CommandLine commandLine) {
				System.out.println("- onBeforeCommandLineProcessing");
			}
		});
		
		app.setGetBrowserProcessHandler(new CEF.App.GetBrowserProcessHandler() {
//			@Override
//			public BrowserProcessHandler getBrowserProcessHandler(Pointer app) {
//				System.out.println("- getBrowserProcessHandler");
//				return browserProcessHandler;
//			}
			@Override
			public BrowserProcessHandler invoke(Pointer app) {
				System.out.println("- getBrowserProcessHandler");
				return null;
			}
		});
		return app;
	}

	public static MainArgs createMainArgs(String[] args, jnr.ffi.Runtime runtime) {
		MainArgs main_args = new MainArgs(runtime);
		main_args.argc.set(args.length + 1);

		Pointer[] array = new Pointer[args.length + 1];
        for (int i = 0; i < array.length; i++) {
        	String argv = (i == 0) ? "cef" : args[i];
			array[i] = Memory.allocateDirect(runtime, argv.length());
            array[i].putString(0, argv, argv.length(), Charset.defaultCharset());
        }

		jnr.ffi.Pointer stringp = Memory.allocateDirect(runtime, array.length * (runtime.addressSize()));
		stringp.put(0, array, 0, array.length);
		main_args.argv.set(stringp);
		return main_args;
	}

	private static String prepareLauncher() {
		File file = new File("subprocess.sh");
		if (!file.exists()) {
	//		String cwd = System.getProperty("user.dir");
			String ld = System.getProperty("java.library.path");
			String cp = System.getProperty("java.class.path");
			String main = System.getProperty("sun.java.command");
			String javaHome = System.getProperty("java.home");
			
			StringBuilder buffer = new StringBuilder("#! /bin/bash\n")
				.append("CEF_ARGS=$@\n")
				//.append("echo $CEF_ARGS\n")
				.append(javaHome).append("/bin/java")
	//			.append("java")
				.append(" -cp ").append(cp)
				.append(" -Djava.library.path=").append(ld)
				.append(" ").append(main).append(" ").append("$CEF_ARGS");
			
			
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
}
