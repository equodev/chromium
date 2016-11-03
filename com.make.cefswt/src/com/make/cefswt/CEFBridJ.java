package com.make.cefswt;

import java.io.IOException;

import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.demangling.Demangler.Symbol;

import cef.binding.CefLibrary;
import cef.binding.CefMainArgs;

public class CEFBridJ {

	public static void main(String[] args) throws IOException {
		System.out.println(CefLibrary.cef_version_info(4));
		System.out.println(CefLibrary.cef_version_info(5));
		
//		for (Symbol symb : BridJ.getNativeLibrary(CefMainArgs.class).getSymbols()) {
////			System.out.println(symb.getName());
////			System.out.println(symb.getParsedRef());
//		}
		
//		CefMainArgs main_args = new CefMainArgs(args.length, Pointer.pointerToCStrings(args));
//		CefMainArgs main_args = new CefMainArgs();
		Pointer<CefMainArgs> main_args = Pointer.allocate(CefMainArgs.class);
		
		// CefJNR applications have multiple sub-processes (render, plugin, GPU, etc)
		  // that share the same executable. This function checks the command-line and,
		  // if this is a sub-process, executes the appropriate logic.
		  int exit_code = CefLibrary.cef_execute_process(main_args, null, null);
		  if (exit_code >= 0) {
//		    // The sub-process has completed so return here.
		    throw new RuntimeException(exit_code + "");
		  }

		  // Install xlib error handlers so that the application won't be terminated
		  // on non-fatal errors.
//		  XSetErrorHandler(XErrorHandlerImpl);
//		  XSetIOErrorHandler(XIOErrorHandlerImpl);

		  // Specify CefJNR global settings here.
//		  CefSettings settings;

		  // SimpleApp implements application-level callbacks for the browser process.
		  // It will create the first browser instance in OnContextInitialized() after
		  // CefJNR has initialized.
//		  CefRefPtr<SimpleApp> app(new SimpleApp);

		  // Initialize CefJNR for the browser process.
//		  CefLibrary.CefInitialize(main_args, settings, app.get(), NULL);

		  // Run the CefJNR message loop. This will block until CefQuitMessageLoop() is
		  // called.
		  CefLibrary.cef_run_message_loop();

		  // Shut down CefJNR.
		  CefLibrary.cef_shutdown();
	}
	
}
