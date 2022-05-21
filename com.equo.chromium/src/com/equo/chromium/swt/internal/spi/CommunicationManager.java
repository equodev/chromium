package com.equo.chromium.swt.internal.spi;

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;

public interface CommunicationManager {

	public Optional<String> receiveMessage(String message);

	public static CommunicationManager get() {
		ServiceLoader<CommunicationManager> serviceLoader = ServiceLoader.load(CommunicationManager.class,
				CommunicationManager.class.getClassLoader());
		Iterator<CommunicationManager> it = serviceLoader.iterator();
		if (it.hasNext()) {
			return it.next();
		}
		return null;
	}

}
