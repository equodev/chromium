package com.equo.chromium.swt.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.cef.CefClientSwt;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.eclipse.swt.SWTException;

import com.equo.chromium.wl.IBrowser;

public final class Windowless implements IBrowser {
	
	private CefClientSwt clientHandler;
	private CefMessageRouter router;
	private CefBrowser browser;
	private CompletableFuture<Boolean> created = new CompletableFuture<>();

	public Windowless(String url) {
		Engine.initCEF();
		createClient();
		browser = clientHandler.createBrowser(url, true, false, null);
		browser.createImmediately();
	}

	private void createClient() {
		clientHandler = Engine.createClient();
		clientHandler.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
			@Override
			public void onAfterCreated(CefBrowser browser) {
				created.complete(true);
			}
		});
		router = Eval.createRouter();
		clientHandler.addMessageRouter(router);
	}
	
	@Override
	public boolean setUrl(String url) {
		created.thenRun(() -> {
			browser.loadURL(url);
		});
		return true;
	}

	@Override
	public Object evaluate(String script) {
		try {
			return Eval.eval(script, browser.getURL(), browser, router, created);
		} catch (InterruptedException e) {
			throw new SWTException("Script that was evaluated failed");
		} catch (ExecutionException e) {
			throw (SWTException) e.getCause();
		}
	}

	@Override
	public boolean close() {
		browser.setCloseAllowed();
		browser.close(true);
		clientHandler.removeMessageRouter(router);
		router.dispose();
		clientHandler.dispose();
		return true;
	}
}