package x11.xlib;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;

public class X11 {
	
	public static interface XLib {
		
		public static interface XIOErrorHandler {
			@Delegate
			int invoke(Pointer display);
		}

		Pointer XSetIOErrorHandler (XIOErrorHandler handler);
		
		public static interface XErrorHandler {
			@Delegate
			int invoke(Pointer display, Pointer error_event);
		}

		Pointer XSetErrorHandler (XErrorHandler handler);
	}
	
	public static XLib getLib() {
		XLib lib = LibraryLoader.create(XLib.class).load("X11");
		return lib;
	}
}
