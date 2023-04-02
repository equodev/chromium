package com.equo.chromium.internal;


import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserWl;
import org.cef.browser.CefMessageRouter;
import org.cef.misc.Rectangle;

import com.equo.chromium.swt.internal.AbstractEval;

public final class Windowless extends IndependentBrowser {

	private CefMessageRouter router;

	public Windowless(String url) {
		this(url, null);
	}

	public Windowless(String url, Rectangle window) {
		Engine.initCEF();
		createClient();
		setBrowser(getClientHandler().createBrowser(url, true, false, createRequestContext()));
		CefBrowser browser = getBrowser();
		browser.setReference(this);
		browser.createImmediately();
		if (window != null && browser instanceof CefBrowserWl) {
			((CefBrowserWl) browser).setWindow(window);
		}
	}

	@Override
	protected void createClient() {
		super.createClient();
		router = AbstractEval.createRouter();
		getClientHandler().addMessageRouter(router);
	}

	@Override
	public boolean close() {
		getClientHandler().removeMessageRouter(router);
		router.dispose();
		return super.close();
	}

}