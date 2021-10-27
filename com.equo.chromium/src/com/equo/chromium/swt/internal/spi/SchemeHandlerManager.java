package com.equo.chromium.swt.internal.spi;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public interface SchemeHandlerManager {

	public List<SchemeDomainPair> getRegisteredSchemes();

	public SchemeHandler getSchemeHandler(String scheme, String domain);

	public static SchemeHandlerManager get() {
		ServiceLoader<SchemeHandlerManager> serviceLoader = ServiceLoader.load(SchemeHandlerManager.class,
				SchemeHandlerManager.class.getClassLoader());
		Iterator<SchemeHandlerManager> it = serviceLoader.iterator();
		if (it.hasNext()) {
			return it.next();
		}
		return null;
	}

}
