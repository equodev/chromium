/****************************************************************************
**
** Copyright (C) 2022 Equo
**
** This file is part of Equo Chromium.
**
** Commercial License Usage
** Licensees holding valid commercial Equo licenses may use this file in
** accordance with the commercial license agreement provided with the
** Software or, alternatively, in accordance with the terms contained in
** a written agreement between you and Equo. For licensing terms
** and conditions see https://www.equo.dev/terms.
**
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3 as published by the Free Software
** Foundation. Please review the following
** information to ensure the GNU General Public License requirements will
** be met: https://www.gnu.org/licenses/gpl-3.0.html.
**
****************************************************************************/


package com.equo.chromium.swt.internal;

import static com.equo.chromium.internal.Engine.debug;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

public abstract class AbstractEval {

	public static CefMessageRouter createRouter() {
		CefMessageRouterConfig config = new CefMessageRouterConfig("chromiumEvaluate", "chromiumEvaluateCancel");
		return CefMessageRouter.create(config);
	}

	protected static String getEvalFunction(String id, String script, String finallyExec) {
		return "(function() {\n"
				+ "var req;\n"
				+ "try {\n"
				+ "  var ret;\n"
				+ "  try {\n"
				+ "  ret = (function() {\n"
				+ "  "+script+"\n"
				+ "  })();\n"
				+ "  try { req = __encodeType(['"+id+"', ret]) } catch(e) { req = __encodeType("+SWT.ERROR_INVALID_RETURN_VALUE+", true) };\n"
				+ "  } catch(e) { req = __encodeType(e.toString(), true); }\n"
				+ "} finally {"
				+      finallyExec
				+ "  }})();";
	}

	public abstract Object eval(String script, CompletableFuture<Boolean> created) throws InterruptedException, ExecutionException;

	protected Object executeEvalWithHandler(CefMessageRouter router, String url, BiConsumer<String, String> function,
			String script, CompletableFuture<Boolean> created) throws InterruptedException, ExecutionException {

		String id = Integer.toString(new Random().nextInt());
		CompletableFuture<Object> evalResult = new CompletableFuture<>();

		CefMessageRouterHandler handler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {
				debug("evaluate.returned: "+ request);
				try {
					callback.success(null);
					Object[] decodeType = (Object[]) decodeType(request, SWT.ERROR_INVALID_RETURN_VALUE);
					if (!id.equals(decodeType[0]))
						return false;
					evalResult.complete(decodeType[1]);
				} catch (Throwable t) {
					evalResult.completeExceptionally(t);
				}
				return true;
			}
		};
		router.addHandler(handler, true);

		String finallyExec = "  window.chromiumEvaluate({request: req,";
		if (debug) {
			finallyExec += " onSuccess: function(response) { console.log(response); },"
					+ " onFailure: function(error_code, error_message) {console.log(error_message);},";
		}
		finallyExec += " persistent: true });\n";

		String eval = getEvalFunction(id, script, finallyExec);
		created.thenRun(() -> {
			CefApp.getInstance().doMessageLoopWork(-1);
			function.accept(eval, url);
		});
		awaitCondition(Display.getCurrent(), evalResult, false);

		router.removeHandler(handler);
		return evalResult.get();
	}

	protected void awaitCondition(Display display, CompletableFuture<?> condition, boolean doMessageLoopWork) {
		if (display != null) {
			while (!condition.isDone() && !display.isDisposed()) {
				if (doMessageLoopWork)
					CefApp.getInstance().doMessageLoopWork(-1);
				if (!display.readAndDispatch())
					display.sleep();
			}
		}
	}

	public static Object decodeType(String encoded, int errorCode) throws SWTException {
		try {
			Object json = Jsoner.deserialize(encoded);
			return decodeType(json, errorCode);
		} catch (JsonException e) {
			throw new SWTException(SWT.ERROR_INVALID_RETURN_VALUE);
		}
	}

	private static Object decodeType(Object json, int errorCode) {
		if (json instanceof JsonArray) {
			int size = ((JsonArray) json).size();
			Object[] array = new Object[size];
			for (int i = 0; i < array.length; i++) {
				array[i] = decodeType(((JsonArray) json).get(i), errorCode);
			}
			return array;
		}
		else if (json instanceof JsonObject) {
			JsonObject jsonErr = (JsonObject) json;
			if (jsonErr.containsKey("isError")) {
				Object err = jsonErr.get("error");
				if (Integer.valueOf(SWT.ERROR_INVALID_RETURN_VALUE).equals(err)) {
					throw new SWTException(SWT.ERROR_INVALID_RETURN_VALUE);
				}
				throw new SWTException(SWT.ERROR_FAILED_EVALUATE, err.toString());
			}
			throw new SWTException(errorCode);
		}
		else if (json instanceof Number) {
			return ((Number) json).doubleValue();
		}
		return json; // either a boolean, null, Number or String.
	}
}
