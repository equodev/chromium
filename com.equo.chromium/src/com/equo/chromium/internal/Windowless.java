package com.equo.chromium.internal;

import java.util.concurrent.ExecutionException;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserWl;
import org.cef.browser.CefMessageRouter;
import org.cef.misc.Rectangle;

import com.equo.chromium.swt.internal.AbstractEval;
import com.equo.chromium.swt.internal.EvalSimpleImpl;

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
	public Object evaluate(String script) {
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
		getClientHandler().removeMessageRouter(router);
		router.dispose();
		return super.close();
	}

}