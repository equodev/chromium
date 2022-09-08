package com.equo.chromium.swt.internal.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;

public interface ScriptExtension {
	public static String DISABLE_SCRIPT_EXTENSIONS_PROPERTY = "chromium.disable_script_extensions";

	public static Iterator<ScriptExtension> get() {
		ServiceLoader<ScriptExtension> serviceLoader = ServiceLoader.load(ScriptExtension.class,
				ScriptExtension.class.getClassLoader());
		return serviceLoader.iterator();
	}

	public static List<CefMessageRouter> createRouter(List<String> script) {
		final List<CefMessageRouter> scriptExtensions = new ArrayList<CefMessageRouter>();
		script.stream().forEach(nScript -> {
			CefMessageRouterConfig routerConfig = new CefMessageRouterConfig("__scriptExtension", nScript);
			scriptExtensions.add(CefMessageRouter.create(routerConfig));
		});
		return scriptExtensions;
	}

	public List<String> getScriptExtensions();

	public void onClose();

}
