package cef.binding;

import org.bridj.BridJ;
import org.bridj.ann.Library;
import org.bridj.cpp.CPPObject;

@Library("cef") 
public class CefApp extends CPPObject {
	static {
		BridJ.register();
	}
}