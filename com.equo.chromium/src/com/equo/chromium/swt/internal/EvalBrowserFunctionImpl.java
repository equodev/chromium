package com.equo.chromium.swt.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import org.cef.browser.CefMessageRouter;

import com.equo.chromium.swt.internal.Chromium.FunctionsResourceHandler;

public class EvalBrowserFunctionImpl extends AbstractEval {
	private CefMessageRouter router;
	private FunctionsResourceHandler functionsResourceHandler;

	EvalBrowserFunctionImpl(CefMessageRouter router, FunctionsResourceHandler functionsResourceHandler) {
		this.router = router;
		this.functionsResourceHandler = functionsResourceHandler;
	}

	@Override
	public Object eval(String script, CompletableFuture<Boolean> created)
			throws InterruptedException, ExecutionException {
		BiConsumer<String, String> function = (eval, url) -> functionsResourceHandler.setEval(eval);
		return executeEvalWithHandler(router, "", function, script, created);
	}
}
