package com.make.cef.samples.cefsimple;

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
import jnr.ffi.LibraryLoader;
import jnr.ffi.Memory;
import jnr.ffi.NativeLong;
import jnr.ffi.NativeType;
import jnr.ffi.Pointer;
import jnr.ffi.Struct;
import jnr.ffi.byref.PointerByReference;

public class CefJNR {
//	public static interface Cef {
//		int cef_version_info(int entry);
//		
//		int CefExecuteProcess(Pointer args, Pointer application, Pointer windows_sandbox_info);
//		
//		
//	}
	
	public static void main(String[] args) {
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

//		System.out.println("Calling executeProcess");
//		int exit_code = CEF.executeProcess(main_args, null, null);
//		if (exit_code >= 0) {
//			// The sub-process has completed so return here.
//			throw new RuntimeException(exit_code + "");
//		}
//
		  // Install xlib error handlers so that the application won't be terminated
		  // on non-fatal errors.
//		  XSetErrorHandler(XErrorHandlerImpl);
//		  XSetIOErrorHandler(XIOErrorHandlerImpl);

		Pointer st = CEF.INSTANCE.stringUserfreeUtf16Alloc();
//		PointerByReference st = new PointerByReference();
		if (st == null)
			throw new RuntimeException("st null");
//		assert(CEF.INSTANCE.stringUtf16Set("su", 2, st, 1) == 1);
		if (CEF.INSTANCE.stringUtf8ToUtf16("su", new NativeLong(2), st) != 1)
			throw new RuntimeException("stringUtf8ToUtf16");
		
		String s = st.getString(0, 2, Charset.forName("UTF-8"));
		if (!"su".equals(s))
			throw new RuntimeException(s);
		
		// Specify Cef global settings here.
		Settings settings = new CEF.Settings();
		settings.no_sandbox.set(1);
//		settings.resources_dir_path.set("/home/guille/Downloads/cef_binary_3.2704.1434.gec3e9ed_linux64/Resources");
//		settings.locales_dir_path.set("/home/guille/Downloads/cef_binary_3.2704.1434.gec3e9ed_linux64/Resources/locales");
		settings.log_severity.set(LogSeverity.LOGSEVERITY_VERBOSE);
		settings.browser_subprocess_path.set("/home/guille/workspaces/make.cefswt/com.make.cefswt/subp.sh");
//		settings.browser_subprocess_path.set(st);
//		settings.browser_subprocess_path.set("su");
		settings.command_line_args_disabled.set(0);
		settings.single_process.set(0);

		// SimpleApp implements application-level callbacks for the browser process.
		// It will create the first browser instance in OnContextInitialized() after
		// Cef has initialized.

		App app = new App();
		
		BrowserProcessHandler browserProcessHandler = new CEF.BrowserProcessHandler();
		browserProcessHandler.set_on_context_initialized(new CEF.BrowserProcessHandler.OnContextInitialized() {
			@Override
			public void onContextInitialized(Pointer self) {
				System.out.println("onContextInitialized");
			}
		});
		app.set_on_before_command_line_processing(new CEF.App.OnBeforeCommandLineProcessing() {
			@Override
			public void onBeforeCommandLineProcessing(Pointer app, Pointer process_type, jnr.ffi.Pointer command_line) {
				System.out.println("onBeforeCommandLineProcessing");
			}
		});
		
//		app.get_browser_process_handler = main_args.new Pointer();
//		jnr.ffi.Pointer allocate = Memory.allocate(CEF.RUNTIME, NativeType.ADDRESS);
//		app.get_browser_process_handler.set(Struct.getMemory(browserProcessHandler));
		app.set_browser_process_handler(new CEF.App.GetBrowserProcessHandler() {
			@Override
			public BrowserProcessHandler getBrowserProcessHandler(Pointer app) {
				System.out.println("getBrowserProcessHandler");
				return browserProcessHandler;
			}
		});
		
		System.out.println("Calling executeProcess");
		int exit_code = CEF.executeProcess(main_args, null, null);
		if (exit_code >= 0) {
			// The sub-process has completed so return here.
			throw new RuntimeException(exit_code + "");
		}

		System.out.println("Calling initialize");
//		app.get_browser_process_handler.set(Memory.allocate(CEF.RUNTIME, ));
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
}
