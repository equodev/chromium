package com.make.cef.samples.cefsimple;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.Arrays;

import cef.capi.CEF;
import cef.capi.CEF.App;
import cef.capi.CEF.BrowserProcessHandler;
import cef.capi.CEF.LogSeverity;
import cef.capi.CEF.MainArgs;
import cef.capi.CEF.Settings;
import jnr.ffi.Memory;
import jnr.ffi.Pointer;
import jnr.ffi.Struct;

public class CefJNR {
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
		
		jnr.ffi.Runtime runtime = CEF.RUNTIME;
        
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
		settings.resources_dir_path.set("/home/guille/Downloads/cef_binary_3.2704.1434.gec3e9ed_linux64/Resources");
		settings.locales_dir_path.set("/home/guille/Downloads/cef_binary_3.2704.1434.gec3e9ed_linux64/Resources/locales");
		settings.browser_subprocess_path.set(subprocessPath);
		settings.log_file.set(System.getProperty("user.dir") + File.separator + "ceflog.log" );
		settings.log_severity.set(LogSeverity.LOGSEVERITY_VERBOSE);
		settings.no_sandbox.set(1);
//		settings.command_line_args_disabled.set(0);
//		settings.single_process.set(0);
		return settings;
	}

	public static App createApp() {
		App app = new App();
		
		BrowserProcessHandler browserProcessHandler = new CEF.BrowserProcessHandler();
		browserProcessHandler.set_on_context_initialized(new CEF.BrowserProcessHandler.OnContextInitialized() {
			@Override
			public void onContextInitialized(Pointer self) {
				System.out.println("- onContextInitialized");
				
				// Browser settings.
				// It is mandatory to set the "size" member.
				CEF.BrowserSettings browserSettings = new CEF.BrowserSettings();
			    browserSettings.size = sizeof(cef_browser_settings_t);
			    
			    // Client handler and its callbacks.
			    // cef_client_t structure must be filled. It must implement
			    // reference counting. You cannot pass a structure 
			    // initialized with zeroes.
			    cef_client_t client = {};
			    initialize_client_handler(&client);

			    // Create browser.
			    printf("cef_browser_host_create_browser\n");
			    cef_browser_host_create_browser(&windowInfo, &client, &cefUrl,
			            &browserSettings, NULL);

			}
		});
		app.set_on_before_command_line_processing(new CEF.App.OnBeforeCommandLineProcessing() {
			@Override
			public void onBeforeCommandLineProcessing(Pointer app, Pointer process_type, jnr.ffi.Pointer command_line) {
				System.out.println("- onBeforeCommandLineProcessing");
			}
		});
		
		app.set_browser_process_handler(new CEF.App.GetBrowserProcessHandler() {
			@Override
			public BrowserProcessHandler getBrowserProcessHandler(Pointer app) {
				System.out.println("- getBrowserProcessHandler");
				return browserProcessHandler;
			}
		});
		return app;
	}

	public static MainArgs createMainArgs(String[] args, jnr.ffi.Runtime runtime) {
		MainArgs main_args = new MainArgs();
		main_args.argc.set(args.length);

		Pointer[] array = new Pointer[args.length];
        for (int i = 0; i < array.length; i++) {
        	array[i] = Memory.allocateDirect(runtime, args[i].length());
            array[i].putString(0, args[i], args[i].length(), Charset.defaultCharset());
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
