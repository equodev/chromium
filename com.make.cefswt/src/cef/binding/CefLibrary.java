package cef.binding;

import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ann.Library;
import org.bridj.ann.Runtime;
import org.bridj.cpp.CPPRuntime;

@Library("cef") 
@Runtime(CPPRuntime.class) 
public class CefLibrary {
	static {
		BridJ.register();
	}

	public static native int cef_version_info(int i);
	
	///
	// This function should be called from the application entry point function to
	// execute a secondary process. It can be used to run secondary processes from
	// the browser client executable (default behavior) or from a separate
	// executable specified by the CefSettings.browser_subprocess_path value. If
	// called for the browser process (identified by no "type" command-line value)
	// it will return immediately with a value of -1. If called for a recognized
	// secondary process it will block until the process should exit and then return
	// the process exit code. The |application| parameter may be empty. The
	// |windows_sandbox_info| parameter is only used on Windows and may be NULL (see
	// cef_sandbox_win.h for details).
	///
	/*--cef(api_hash_check,optional_param=application,
	        optional_param=windows_sandbox_info)--*/
	public static native int cef_execute_process(Pointer<CefMainArgs> args, Pointer application, Pointer windows_sandbox_info);
	
	///
	// Run the CefJNR message loop. Use this function instead of an application-
	// provided message loop to get the best balance between performance and CPU
	// usage. This function should only be called on the main application thread and
	// only if CefInitialize() is called with a
	// CefSettings.multi_threaded_message_loop value of false. This function will
	// block until a quit message is received by the system.
	///
	/*--cef()--*/
	public static native void cef_run_message_loop();

	///
	// This function should be called on the main application thread to shut down
	// the CefJNR browser process before the application exits.
	///
	/*--cef()--*/
	public static native void cef_shutdown();
	
}
