package com.make.cefswt;

import jnr.ffi.LibraryLoader;

public class JNR {
	public static interface LibC {
		int puts(String s);
	}
	
	public static interface GetPid {
		long getpid();
	}

	public static void main(String[] args) {
		LibC libc = LibraryLoader.create(LibC.class).load("c");

		libc.puts("Hello, World");
		
		GetPid getPid = LibraryLoader.create(GetPid.class).load("c");
		System.out.println(getPid.getpid());
	}
}
