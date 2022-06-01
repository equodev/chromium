package com.equo.chromium.swt.internal;

import static com.equo.chromium.swt.internal.Engine.debug;

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

public abstract class AbstractEval implements Eval {

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
		awaitCondition(Display.getCurrent(), created, true);
		function.accept(eval, url);
		awaitCondition(Display.getCurrent(), evalResult, false);

		router.removeHandler(handler);
		return evalResult.get();
	}

	protected void awaitCondition(Display display, CompletableFuture<?> condition, boolean doMessageLoopWork) {
		while (!condition.isDone() && !display.isDisposed()) {
			if (doMessageLoopWork)
				CefApp.getInstance().doMessageLoopWork(-1);
			if (!display.readAndDispatch())
				display.sleep();
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
