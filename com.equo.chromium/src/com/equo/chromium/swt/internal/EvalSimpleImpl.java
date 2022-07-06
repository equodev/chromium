package com.equo.chromium.swt.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;

public class EvalSimpleImpl extends AbstractEval {
	private String url;
	private CefMessageRouter router;
	private CefBrowser cefBrowser;

	public EvalSimpleImpl(CefBrowser cefBrowser, CefMessageRouter router, String url) {
		this.url = url;
		this.router = router;
		this.cefBrowser = cefBrowser;
	}

	@Override
	public Object eval(String script, CompletableFuture<Boolean> created)
			throws InterruptedException, ExecutionException {
		BiConsumer<String, String> function = (eval, url) -> cefBrowser.executeJavaScript(eval, url, 1);
		return executeEvalWithHandler(router, url, function, script, created);
	}
}
