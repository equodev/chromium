package jnr.ffi.util.ref.internal;

import java.lang.ref.ReferenceQueue;

public class Finalizer {

	  public static ReferenceQueue<Object> startFinalizer(
	      Class<?> finalizableReferenceClass, Object frq) {
		  System.out.println("DON'T startFinalizer");
		  return null;
	  }
}
