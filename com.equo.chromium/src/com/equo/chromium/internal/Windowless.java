package com.equo.chromium.internal;

import java.util.concurrent.ExecutionException;

import org.cef.CefApp;
import org.cef.CefAppStandalone;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserWl;
import org.cef.browser.CefMessageRouter;
import org.cef.misc.Rectangle;

import com.equo.chromium.internal.Engine.BrowserType;
import com.equo.chromium.swt.internal.AbstractEval;
import com.equo.chromium.swt.internal.EvalSimpleImpl;

public final class Windowless extends IndependentBrowser {

	private CefMessageRouter router;

	public Windowless(String url) {
		this(url, null);
	}

	public Windowless(String url, Rectangle window) {
		Engine.initCEF(BrowserType.STANDALONE);
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
		if (!(CefApp.getWindowingToolkit() instanceof CefAppStandalone)) {
			router = AbstractEval.createRouter();
			getClientHandler().addMessageRouter(router);
		}
	}

	@Override
	public Object evaluate(String script) {
		if (CefApp.getWindowingToolkit() instanceof CefAppStandalone) {
			throw new UnsupportedOperationException("Evaluate is not supported outside of SWT");
		}
		CefBrowser browser = getBrowser();
		try {
			EvalSimpleImpl eval = new EvalSimpleImpl(browser, router, "");
			return eval.eval(script, isCreated());
		} catch (InterruptedException e) {
			throw new RuntimeException("Script that was evaluated failed");
		} catch (ExecutionException e) {
			throw (RuntimeException)e.getCause();
		}
	}
	
	@Override
	public boolean close() {
		if (router != null) {
			getClientHandler().removeMessageRouter(router);
			router.dispose();
		}
		return super.close();
	}

}
