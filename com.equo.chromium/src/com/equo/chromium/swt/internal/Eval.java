package com.equo.chromium.swt.internal;

import static com.equo.chromium.swt.internal.Engine.debug;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

class Eval {
	
	static final String encodeFn = "function encodeType(ret, isError) {\n"
			+ "  if (ret === undefined) ret = null;"
			+ "  if (isError)\n"
			+ "    return JSON.stringify({isError: true, error: ret});\n"
			+ "  return JSON.stringify(ret);\n"
			+ "}\n";
	
	static CefMessageRouter createRouter() {
		CefMessageRouterConfig config = new CefMessageRouterConfig("chromiumEvaluate", "chromiumEvaluateCancel");
		return CefMessageRouter.create(config);
	}

	static Object eval(String script, String url, CefBrowser cefBrowser, CefMessageRouter router) throws InterruptedException, ExecutionException {
		CompletableFuture<Object> evalResult = new CompletableFuture<>();
		
		CefMessageRouterHandler handler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {
				debug("evaluate.returned: "+ request);
				try {
					callback.success(null);
					evalResult.complete(decodeType(request, SWT.ERROR_INVALID_RETURN_VALUE));
				} catch (Throwable t) {
					evalResult.completeExceptionally(t);
				}
				return true;
			}
		};
		router.addHandler(handler, true);

		String eval = "(function() {\n"
				+ "var req;\n"
				+ "try {\n"
				+ "  "+encodeFn
				+ "  var ret;\n"
				+ "  try {\n"
				+ "  ret = (function() {\n"
				+ "  "+script+"\n"
				+ "  })();\n"
				+ "  console.log('eval return: '+ret);\n"
				+ "  try { req = encodeType(ret) } catch(e) { req = encodeType("+SWT.ERROR_INVALID_RETURN_VALUE+", true) };\n"
				+ "  } catch(e) { req = encodeType(e.toString(), true); }\n"
				+ "} finally {"
				+ "  window.chromiumEvaluate({request: req,\n"
				+ "                     persistent: true,\n"
				+ "                     onSuccess: function(response) { console.log(response); },\n"
				+ "                     onFailure: function(error_code, error_message) {console.log(error_message);} });\n"
				+ "}})();";
		cefBrowser.executeJavaScript(eval, url, 1);
		
		Display display = Display.getCurrent();
		while (!evalResult.isDone() && !display.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		
		router.removeHandler(handler);
		
		return evalResult.get();
	}
	
	static Object decodeType(String encoded, int errorCode) throws SWTException {
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
