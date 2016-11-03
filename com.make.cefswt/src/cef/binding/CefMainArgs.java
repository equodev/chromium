package cef.binding;

import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ann.Constructor;
import org.bridj.ann.Library;
import org.bridj.cpp.CPPObject;

@Library("cef") 
public class CefMainArgs extends CPPObject {
	static {
		BridJ.register();
	}
	/**
	 * Original signature : <code>CefMainArgs(int, char**)</code><br>
	 * <i>native declaration : /home/guille/Downloads/cef_binary_3.2704.1434.gec3e9ed_linux64/include/internal/cef_linux.h:26</i>
	 */
	@Constructor(0) 
	public CefMainArgs(int argc_arg, Pointer<Pointer<Byte > > argv_arg) {
		super((Void)null, 0, argc_arg, argv_arg);
	}
	public CefMainArgs() {
		super();
	}
	public CefMainArgs(Pointer pointer) {
		super(pointer);
	}
}
