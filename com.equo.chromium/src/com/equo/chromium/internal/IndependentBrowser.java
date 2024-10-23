package com.equo.chromium.internal;

import static com.equo.chromium.internal.Engine.debug;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.cef.CefClient;
import org.cef.CefSettings.LogSeverity;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefCallback;
import org.cef.callback.CefCompletionCallback;
import org.cef.callback.CefDevToolsMessageObserverAdapter;
import org.cef.callback.CefPdfPrintCallback;
import org.cef.callback.CefQueryCallback;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefFindHandler;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandler.ErrorCode;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.handler.CefPrintHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.misc.CefPdfPrintSettings;
import org.cef.misc.Rectangle;
import org.cef.network.CefRequest.TransitionType;

import com.equo.chromium.ChromiumBrowser;
import com.equo.chromium.Storage;
import com.equo.chromium.internal.Engine.BrowserType;
import com.equo.chromium.swt.internal.spi.CommRouterHandler;
import com.equo.chromium.swt.internal.spi.CommunicationManager;
import com.equo.chromium.swt.internal.spi.ScriptExtension;
import com.equo.chromium.utils.EventAction;
import com.equo.chromium.utils.EventType;
import com.equo.chromium.utils.PdfPrintSettings;
import com.equo.chromium.utils.StorageType;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

public abstract class IndependentBrowser implements ChromiumBrowser {
	private CefClient clientHandler;
	private CefBrowser browser;
	private CefRequestContext requestContext;
	private CompletableFuture<Boolean> created = new CompletableFuture<>();
	private boolean ignoreCertificateErrors = false;
	private List<ConsoleListener> consoleListeners = new ArrayList<ConsoleListener>();
	private String lastSearch = null;
	private static final String DATA_TEXT_URL = "data:text/html;base64,";
	private Storage localStorage;
	private Storage sessionStorage;
	private static ExecutorService executor = null;
	private int messageId = 0;
	private volatile Subscriber subscriber = null;

	public Subscriber getSubscriber() {
		if (subscriber == null) {
			synchronized (this) {
				if (subscriber == null) {
					subscriber = new Subscriber(this);
				}
			}
		}
		return subscriber;
	}

	private ExecutorService getExecutor() {
		if (executor == null) {
			synchronized (IndependentBrowser.class) {
				if (executor == null) {
					executor = Executors.newSingleThreadExecutor(r -> {
						Thread thread = new Thread(r, "chromium-MessageRoute");
						thread.setDaemon(true);
						return thread;
					});
				}
			}
		}
		return executor;
	}

	@Override
	public CompletableFuture<Boolean> isCreated() {
		return created;
	}

	protected CefBrowser getBrowser() {
		return browser;
	}

	protected void setBrowser(CefBrowser browser) {
		this.browser = browser;
	}

	protected CefClient getClientHandler() {
		return clientHandler;
	}

	protected void setClientHandler(CefClient clientHandler) {
		this.clientHandler = clientHandler;
	}

	protected void createClient() {
		clientHandler = Engine.createClient();
		clientHandler.setDisposeAction(() -> {
			CefBrowser browser = getBrowser();
			if (browser != null) {
				browser.setReference(null);
				disposeInBrowser();
			}
			clientHandler.removeLifeSpanHandler();
			clientHandler.removeDisplayHandler();
			clientHandler.removeRequestHandler();
			clientHandler.removeLoadHandler();
			clientHandler.removeFindHandler();
			clientHandler.removePrintHandler();
			clientHandler.removeDialogHandler();
			clientHandler.removeFocusHandler();
			clientHandler.removeDownloadHandler();
			clientHandler.removeJSDialogHandler();
			if (subscriber != null) {
				subscriber.dispose();
				subscriber = null;
			}
			if (executor != null && !getExecutor().isShutdown()) {
				getExecutor().shutdown();
				executor = null;
			}
		});
		clientHandler.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
			@Override
			public void onAfterCreated(CefBrowser browser) {
				getSubscriber().onAfterCreatedNotify(browser);
			}
		});
		clientHandler.addRequestHandler(new CefRequestHandlerAdapter() 
		{
			@Override
			public boolean onCertificateError(CefBrowser browser, ErrorCode cert_error, String request_url,
					CefCallback callback) {
				if (isIgnoreCertificateErrors()) {
					callback.Continue();
					return true;
				}
				if (handleCertificateProperty())
					return false;
				callback.cancel();
				return true;
			}

		});
		clientHandler.addDisplayHandler(new CefDisplayHandlerAdapter() {
			@Override
			public boolean onConsoleMessage(CefBrowser browser, LogSeverity level, String message, String source,
					int line) {
				getSubscriber().onConsoleMessageNotify(level, message, source, line);
				return IndependentBrowser.this.onConsoleMessage(browser, level, message, source, line);
			}

			@Override
			public void onFullscreenModeChange(CefBrowser browser, boolean fullscreen) {
				getSubscriber().onFullscreenModeChangeNotify(fullscreen);
			}
		});
		clientHandler.addLoadHandler(new CefLoadHandler() {
			@Override
			public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
				getSubscriber().onLoadingStateChangeNotify(isLoading, browser.getURL());
			}

			@Override
			public void onLoadStart(CefBrowser browser, CefFrame frame, TransitionType transitionType) {
				getSubscriber().onLoadStartNotify();
			}
			
			@Override
			public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText,
					String failedUrl) {
				getSubscriber().onLoadErrorNotify(errorCode.getCode());
			}

			@Override
			public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
				getSubscriber().onLoadEndNotify(frame);
			}
		});
		clientHandler.addFindHandler(new CefFindHandler() {
			@Override
			public void onFindResult(CefBrowser browser, int identifier, int count, Rectangle selectionRect,
					int activeMatchOrdinal, boolean finalUpdate) {
				if (finalUpdate) {
					getSubscriber().onFindResultNotify(count, activeMatchOrdinal);
				}
			}
		});
		clientHandler.addDisplayHandler(new CefDisplayHandlerAdapter() {
			@Override
			public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
				getSubscriber().onAddressChangeNotify(browser.isLoading(), url);
			}
		});
		clientHandler.addPrintHandler(new CefPrintHandlerAdapter() { });
		CommunicationManager commManager = CommunicationManager.get();
		if (commManager != null) {
			CefMessageRouter commRouter = CommRouterHandler.createRouter();
			commRouter.addHandler(new CommRouterHandler(commManager), true);
			clientHandler.addMessageRouter(commRouter);
		}

		if (!Boolean.getBoolean(ScriptExtension.DISABLE_SCRIPT_EXTENSIONS_PROPERTY)) {
			Iterator<ScriptExtension> scriptExtensions = ScriptExtension.get();
			scriptExtensions.forEachRemaining(scriptExtensionProvider -> {
				List<CefMessageRouter> routers = ScriptExtension
						.createRouter(scriptExtensionProvider.getScriptExtensions());
				routers.stream().forEach(router -> clientHandler.addMessageRouter(router));
			});
		}
	}

	protected void disposeInBrowser() {

	};

	public static void subscribeOnAfterCreated(EventAction eventAction) {
		Subscriber.subscribeOnAfterCreated(eventAction);
	}

	public long subscribe(EventType eventType, EventAction action) {
		return getSubscriber().subscribe(eventType, action);
	}

	public boolean unSubscribe(long idEvent) {
		return getSubscriber().unSubscribe(idEvent);
	}

	public void unSubscribeAll() {
		getSubscriber().unSubscribeAll();
	}

	protected CefRequestContext createRequestContext() {
		requestContext = isIgnoreCertificateErrors() ? CefRequestContext.createContext(null) : null;
		return requestContext;
	}

	protected CefRequestContext getRequestContext() {
		return requestContext;
	};

	protected boolean handleCertificateProperty() {
		String derpem = System.getProperty("chromium.ssl", "");
		if (!derpem.isEmpty()) {
			if (Files.isReadable(Paths.get(derpem))) {
				try {
					byte[] derpemBytes = Files.readAllBytes(Paths.get(derpem));
					System.setProperty("chromium.ssl.cert",
							new String(derpemBytes, "ASCII").replaceAll("\\r\\n", "\n"));
				} catch (IOException e) {
					debugPrint("Failed to read file " + derpem);
					e.printStackTrace();
				}
			} else {
				debugPrint("Cannot read file '" + derpem + "', trying as string");
				System.setProperty("chromium.ssl.cert", derpem.replaceAll("\\r\\n", "\n"));
			}
			return true;
		}
		return false;
	}

	private void debugPrint(String log) {
		Engine.debug(log, getBrowser());
	}

	protected boolean onConsoleMessage(CefBrowser browser, LogSeverity level, String message, String source, int line) {
		boolean prevent = false;
		for (ConsoleListener listener : consoleListeners) {
			if (listener.message(level.ordinal(), message, source, line))
				prevent = true;
		}
		return prevent;
	}

	@Override
	public boolean setUrl(String url) {
		created.thenRun(() -> {
			browser.loadURL(url);
		});
		return true;
	}

	@Override
	public boolean setText(String html) {
		created.thenRun(() -> {
			String texturl = DATA_TEXT_URL + Base64.getEncoder().encodeToString(html.getBytes());
			browser.loadURL(texturl);
		});
		return true;
	}

	@Override
	public void find(String search, boolean forward, boolean matchCase) {
		// Stop finding when search is empty.
		if (search == null || search.isEmpty()) {
			getBrowser().stopFinding(true);
		} else {
			// Stop the search when the search text is changed, so that the new search
			// starts at the first result.
			if (lastSearch != null && !lastSearch.contains(search)) {
				getBrowser().stopFinding(true);
			}
			getBrowser().find(/*1,*/ search, forward, matchCase, true);
			lastSearch = search;
		}
	}

	@Override
	public void zoom(double zoomLevel) {
		getBrowser().setZoomLevel(zoomLevel);
		getSubscriber().notifySubscribers(EventType.onZoomChanged);
	}

	@Override
	public double getZoom() {
		return getBrowser().getZoomLevel();
	}

	@Override
	public void executeJavaScript(String script) {
		created.thenRun(() -> {
			getBrowser().executeJavaScript(script, "", 0);
		});
	}

	@Override
	public void executeJavacript(String script) {
		executeJavaScript(script);
	}

	@Override
	public void addConsoleListener(ConsoleListener listener) {
		consoleListeners.add(listener);
	}

	@Override
	public void removeConsoleListener(ConsoleListener listener) {
		consoleListeners.remove(listener);
	}

	@Override
	public CompletableFuture<byte[]> captureScreenshot() {
		return captureScreenshot(0, 0, 0, 0, 1);
	}

	@Override
	public CompletableFuture<byte[]> captureScreenshot(int x, int y, int width, int height, int scale) {
		CompletableFuture<byte[]> screenshotResult = new CompletableFuture<>();
		new CefDevToolsMessageObserverAdapter(getBrowser()) {
			@Override
			public void onDevToolsMethodResult(CefBrowser cefBrowser, int messageId, boolean success, String result,
					int resultSize) {
				try {
					JsonObject json = (JsonObject) Jsoner.deserialize(result);
					screenshotResult.complete(
							(byte[]) ((String) json.getOrDefault("data", "")).getBytes(StandardCharsets.UTF_8));
				} catch (JsonException e) {
					screenshotResult.complete("".getBytes(StandardCharsets.UTF_8));
				} finally {
					dispose();
				}
			}
		};
		JsonObject jsonMessage = new JsonObject();
		jsonMessage.put("id", messageId++);
		jsonMessage.put("method", "Page.captureScreenshot");
		JsonObject clip = new JsonObject();
		if (width > 0 && height > 0) {
			JsonObject viewport = new JsonObject();
			viewport.put("x", x);
			viewport.put("y", y);
			viewport.put("width", width);
			viewport.put("height", height);
			viewport.put("scale", scale);
			clip.put("clip", viewport);
			jsonMessage.put("params", clip);
		}
		String message = jsonMessage.toJson();
		getBrowser().sendDevToolsMessage(message, message.length());
		return screenshotResult;
	}

	@Override
	public void ignoreCertificateErrors(boolean enable) {
		ignoreCertificateErrors = enable;
		if (!enable && getBrowser() != null) {
			CefCompletionCallback callback = new CefCompletionCallback() {
				@Override
				public void onComplete() {
					debug("certificate exceptions cleared");
				}
			};
			if (getRequestContext() == null) {
				CefRequestContext.getGlobalContext().clearCertificateExceptions(callback);
			} else {
				getRequestContext().clearCertificateExceptions(callback);
			}
		}
	}

	public boolean isIgnoreCertificateErrors() {
		return ignoreCertificateErrors;
	}

	@Override
	public List<Object> getErrors() {
		return new ArrayList<>();
	}

	@Override
	public boolean close() {
		isCreated().thenRun(() -> {
			CefBrowser browser = getBrowser();
			if (browser != null) {
				browser.setCloseAllowed();
				browser.close(true);
				CefClient client = browser.getClient();
				if ( client != null) {
					client.dispose();
				}
			}
		});
		return true;
	}

	@Override
	public Object getUIComponent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isLoading() {
		return getBrowser().isLoading();
	}

	@Override
	public void goBack() {
		getBrowser().goBack();
	}

	@Override
	public void goForward() {
		getBrowser().goForward();
	}

	@Override
	public boolean canGoBack() {
		return getBrowser().canGoBack();
	}

	@Override
	public boolean canGoForward() {
		return getBrowser().canGoForward();
	}

	@Override
	public void reload() {
		getBrowser().reload();
	}

	@Override
	public void stop() {
		getBrowser().stopLoad();
	}

	@Override
	public CompletableFuture<String> text() {
		CompletableFuture<String> result = new CompletableFuture<String>();
		getBrowser().getMainFrame().getSource(new CefStringVisitor() {
			
			@Override
			public void visit(String text) {
				result.complete(text);
			}
		});
		return result;
	}

	@Override
	public String getUrl() {
		return getBrowser().getURL();
	}

	public CompletableFuture<Object> sendDevToolsMessage(String devToolsMethod, List<Map.Entry<String, Object>> params, String wanted) {
		CompletableFuture<Object> messageResult = new CompletableFuture<>();
		created.thenRun(() -> {
			int id = messageId++;
			new CefDevToolsMessageObserverAdapter(getBrowser()) {
				@Override
				public void onDevToolsMethodResult(CefBrowser cefBrowser, int messageId, boolean success, String result, int resultSize) {
					debugPrint("onDevToolsMethodResult: " +result);
					if (id != messageId) {
						return;
					}
					try {
						JsonObject json = (JsonObject) Jsoner.deserialize(result);
						messageResult.complete(wanted == null || wanted.isEmpty() ? json : json.get(wanted));
					} catch (JsonException e) {
						messageResult.completeExceptionally(e.getCause());
					} finally {
						dispose();
					}
				}
			};
			JsonObject jsonMessage = new JsonObject();
			jsonMessage.put("id", id);
			jsonMessage.put("method", devToolsMethod);
			jsonMessage.put("params", createJsonObject(params));
			String devToolsMessage = jsonMessage.toJson();
			getBrowser().sendDevToolsMessage(devToolsMessage, devToolsMessage.length());
		});
		return messageResult;
	}

	private JsonObject createJsonObject(List<Map.Entry<String, Object>> params) {
		JsonObject jsonParams = new JsonObject();
		params.forEach((entry) -> {
			Object value = entry.getValue();
			if (value == null) {
				jsonParams.put(entry.getKey(), value);
			} else if (value instanceof Integer) {
				jsonParams.put(entry.getKey(), (Integer) value);
			} else if (value instanceof String) {
				jsonParams.put(entry.getKey(), (String) value);
			} else if (value instanceof Boolean) {
				jsonParams.put(entry.getKey(), (Boolean) value);
			} else if (value instanceof List<?>) {
				
			}
		});
		return jsonParams;
	}

	public void addMessageRoute(String queryFunctionName, String cancelQueryFunctionName,
			Function<String, String> result) {
		isCreated().thenRun(() -> {
			CefMessageRouterConfig config = new CefMessageRouterConfig(queryFunctionName, cancelQueryFunctionName);
			CefMessageRouter messageRouter_ = CefMessageRouter.create(config);
			CefMessageRouterHandler newHandler = new CefMessageRouterHandlerAdapter() {
				@Override
				public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
						CefQueryCallback callback) {
					try {
						getExecutor().submit(() -> callback.success(result.apply(request)));
						return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
					return false;
				}

			};
			messageRouter_.addHandler(newHandler, false);
			getClientHandler().addMessageRouter(messageRouter_);
		});
	}

	public void showDevTools() {
		isCreated().thenRun(() -> {
			getBrowser().getDevTools().createImmediately();
		});
	}

	public CompletableFuture<Boolean> printToPdf(String path, PdfPrintSettings settings) {
		CompletableFuture<Boolean> result = new CompletableFuture<Boolean>();
		CefPdfPrintSettings pdfPrintSettings = new CefPdfPrintSettings();
		if (settings != null) {
			pdfPrintSettings.display_header_footer = settings.display_header_footer;
			pdfPrintSettings.footer_template = settings.footer_template;
			pdfPrintSettings.header_template = settings.header_template;
			pdfPrintSettings.landscape = settings.landscape;
			pdfPrintSettings.print_background = settings.print_background;
			pdfPrintSettings.page_ranges = settings.page_ranges;
			pdfPrintSettings.paper_width = settings.paper_width;
			pdfPrintSettings.paper_height = settings.paper_height;
			pdfPrintSettings.prefer_css_page_size = settings.prefer_css_page_size;
			pdfPrintSettings.scale = settings.scale;
			pdfPrintSettings.margin_top = settings.margin_top;
			pdfPrintSettings.margin_right = settings.margin_right;
			pdfPrintSettings.margin_bottom = settings.margin_bottom;
			pdfPrintSettings.margin_left = settings.margin_left;
			if (settings.margin_type != null) {
				pdfPrintSettings.margin_type = CefPdfPrintSettings.MarginType.valueOf(settings.margin_type.name());
			}
		}

		isCreated().thenRun(() -> {
			getBrowser().printToPDF(path, pdfPrintSettings, new CefPdfPrintCallback() {
				@Override
				public void onPdfPrintFinished(String path, boolean ok) {
					result.complete(ok);
				}
			});
		});
		return result;
	}

	public CompletableFuture<Boolean> printToPdf(String path) {
		return printToPdf(path, null);
	}

	public static void checkToolkit(BrowserType browserType) {
		if (Engine.browserTypeInitialized == null || browserType == Engine.browserTypeInitialized) {
			return;
		}
		throw new UnsupportedOperationException("You cannot initialize such browsers, because browsers of type "
				+ Engine.browserTypeInitialized + " are already initialized.");
	}

	public Storage getLocalStorage() {
		if (localStorage == null) {
			synchronized (Storage.class) {
				if (localStorage == null) {
					localStorage = new Storage(this, StorageType.LOCAL);
				}
			}
		}
		return localStorage;
	}

	public Storage getSessionStorage() {
		if (sessionStorage == null) {
			synchronized (Storage.class) {
				if (sessionStorage == null) {
					sessionStorage = new Storage(this, StorageType.SESSION);
				}
			}
		}
		return sessionStorage;
	}
}