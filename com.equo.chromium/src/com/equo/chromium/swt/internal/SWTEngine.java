package com.equo.chromium.swt.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import com.equo.chromium.internal.Utils;

public class SWTEngine {

	
	public static void initCef(AtomicBoolean closing, AtomicBoolean shuttingDown, Runnable shutdownRunnable) {
		Display.getDefault().asyncExec(() -> {
			Display.getDefault().addListener(SWT.Close, e -> {
				closing.set(true);
			});
			Display.getDefault().disposeExec(() -> {
				if (shuttingDown.get()) {
					// already shutdown
					return;
				}
				if (!Utils.isMac() || !closing.get()) {
					shuttingDown.set(true);
					shutdownRunnable.run();
				}
			});
		});
	}

	public static void onContextInitialized(CefApp app) {
		registerBrowserFunctions(app);
	}

	public static void registerBrowserFunctions(CefApp app) {
		app.registerSchemeHandlerFactory("https", "functions", new CefSchemeHandlerFactory() {
			@Override
			public CefResourceHandler create(CefBrowser browser, CefFrame frame,
					String schemeName, CefRequest request) {
				if (!browser.isPopup()) {
					if (isPartial(request))
						return Chromium.getChromium(browser).functionsResourceHandler.peek();
					return Chromium.getChromium(browser).createFunctionResourceHandler();
				}
				return null;
			}
			
			protected boolean isPartial(CefRequest request) {
				try {
					URI url = new URI(request.getURL());
					return url.getFragment() != null;
				} catch (URISyntaxException e) {
					return false;
				}
			}
		});
	}

	public static boolean isSystemDarkTheme() {
		boolean isDarkTheme = false;
		try {
			Class<?> systemThemeClass = Class.forName("org.eclipse.swt.widgets.Display");
			// Method available since SWT 3.112
			Method isSystemDarkThemeMethod = systemThemeClass.getMethod("isSystemDarkTheme");
			if (isSystemDarkThemeMethod != null) {
				isDarkTheme = (boolean) isSystemDarkThemeMethod.invoke(null);
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException
				| NoSuchMethodException | SecurityException e) {
		}
		return isDarkTheme;
	}
	
	public static int getSWTVersion() {
		return SWT.getVersion();
	}

}
