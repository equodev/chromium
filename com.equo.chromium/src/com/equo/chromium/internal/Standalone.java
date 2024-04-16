package com.equo.chromium.internal;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserStandalone;
import org.cef.misc.Rectangle;

public final class Standalone extends IndependentBrowser {

	private void init(String url) {
		Engine.initCEF(Engine.BrowserType.STANDALONE);
		createClient();
		setBrowser(getClientHandler().createBrowser(url, false, false, createRequestContext(), null));
		getBrowser().setReference(this);
	}

	public Standalone(String url) {
		init(url);
		getBrowser().createImmediately();
	}

	public Standalone(String url, Rectangle window) {
		init(url);
		CefBrowser browser = getBrowser();
		if (browser instanceof CefBrowserStandalone) {
			((CefBrowserStandalone) browser).setWindow(window);
		}
		browser.createImmediately();
	}

}