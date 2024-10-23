package com.equo.chromium.internal;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserWl;
import org.cef.misc.Rectangle;

public final class Windowless extends IndependentBrowser {

	public Windowless(String url) {
		this(url, null);
	}

	public Windowless(String url, Rectangle window) {
		Engine.initCEF(Engine.getBrowserType());
		createClient();
		setBrowser(getClientHandler().createBrowser(url, true, false, createRequestContext(), null));
		CefBrowser browser = getBrowser();
		browser.setReference(this);
		browser.createImmediately();
		if (window != null && browser instanceof CefBrowserWl) {
			((CefBrowserWl) browser).setWindow(window);
		}
	}
}