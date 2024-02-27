package com.equo.chromium.swt.internal.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

public interface SecurityManager {

	public boolean isEnabled();

	public static SecurityManager get() {
		ServiceLoader<SecurityManager> serviceLoader = ServiceLoader.load(SecurityManager.class,
				SecurityManager.class.getClassLoader());
		Iterator<SecurityManager> it = serviceLoader.iterator();
		if (it.hasNext()) {
			SecurityManager securityManager = it.next();
			if (securityManager != null
					&& securityManager.getClass().getName().startsWith("com.equo.chromium.swt.internal.spi.")) {
				return securityManager;
			}
		}
		return null;
	}
}