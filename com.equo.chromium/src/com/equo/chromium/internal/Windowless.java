package com.equo.chromium.internal;

import java.lang.reflect.Method;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserWl;
import org.cef.misc.Rectangle;

import com.equo.chromium.ChromiumBrowser;
import com.equo.chromium.internal.Engine.BrowserType;

public final class Windowless extends IndependentBrowser {

	public Windowless(String url) {
		this(url, null);
	}

	public Windowless(String url, Rectangle window) {
		Engine.initCEF(getBrowserType());
		createClient();
		setBrowser(getClientHandler().createBrowser(url, true, false, createRequestContext()));
		CefBrowser browser = getBrowser();
		browser.setReference(this);
		browser.createImmediately();
		if (window != null && browser instanceof CefBrowserWl) {
			((CefBrowserWl) browser).setWindow(window);
		}
	}

	private static BrowserType getBrowserType() {
		if (Boolean.getBoolean("chromium.force_windowless_swt")) {
			return BrowserType.SWT;
		}
		if (Boolean.getBoolean("chromium.force_windowless_headless")) {
			return BrowserType.HEADLESS;
		}

		try {
			Class<?> clazz = Class.forName("org.eclipse.swt.widgets.Display", false,
					ChromiumBrowser.class.getClassLoader());
			if (clazz != null) {
				Method findDisplay = clazz.getDeclaredMethod("findDisplay", Thread.class);
				for (Thread thread : Thread.getAllStackTraces().keySet()) {
					if (findDisplay.invoke(null, thread) != null) {
						return BrowserType.SWT;
					}
				}
			}
		} catch (Throwable e) {
		}

		return BrowserType.HEADLESS;
	}
}