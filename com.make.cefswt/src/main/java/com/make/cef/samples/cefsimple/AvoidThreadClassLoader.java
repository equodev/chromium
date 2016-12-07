package com.make.cef.samples.cefsimple;

public class AvoidThreadClassLoader extends ClassLoader {
	public AvoidThreadClassLoader(ClassLoader parent) {
		super(parent);
	}
	
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
//		if ("jnr.ffi.util.ref.internal.Finalizer".equals(name)) {
//			
//		}
		return super.loadClass(name, resolve);
	}
}