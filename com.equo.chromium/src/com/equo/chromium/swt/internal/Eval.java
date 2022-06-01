package com.equo.chromium.swt.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

interface Eval {
	public Object eval(String script, CompletableFuture<Boolean> created) throws InterruptedException, ExecutionException;
}
