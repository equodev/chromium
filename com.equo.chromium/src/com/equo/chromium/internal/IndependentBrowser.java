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


package com.equo.chromium.internal;

import static com.equo.chromium.internal.Engine.debug;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
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
import com.equo.chromium.internal.Engine.BrowserType;
import com.equo.chromium.swt.internal.spi.CommRouterHandler;
import com.equo.chromium.swt.internal.spi.CommunicationManager;
import com.equo.chromium.swt.internal.spi.ScriptExtension;
import com.equo.chromium.utils.EventAction;
import com.equo.chromium.utils.EventType;
import com.equo.chromium.utils.PdfPrintSettings;
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
	private Map<EventType, List<EventAction>> subscribeEvents = new HashMap<>();
	private Map<Long, ActionData> subscribeIndex = new HashMap<>();
	private long eventId = 0;
	private static ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "chromium-MessageRoute");
		thread.setDaemon(true);
		return thread;
	});
	private int messageId = 0;
	protected static EventAction eventActionOfAfterCreated = null;

	class ActionData {
		public EventType eventType;
		public Runnable action;

		public ActionData(EventType eventType, Runnable action) {
			this.eventType = eventType;
			this.action = action;
		}
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
		clientHandler.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
			@Override
			public void onAfterCreated(CefBrowser browser) {
				if (eventActionOfAfterCreated != null && !created.isDone()) {
					Map<String, Object> mapData = new HashMap<>();
					mapData.put("chromium_instance", (ChromiumBrowser)browser.getReference());
					eventActionOfAfterCreated.setJsonData(mapData);
					eventActionOfAfterCreated.run();
				}
				created.complete(true);
				notifySubscribers(EventType.onAfterCreated);
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
				Map<String, Object> mapData = new HashMap<>();
				mapData.put("level", level.toString());
				mapData.put("message", message);
				mapData.put("source", source);
				mapData.put("line", line);
				notifySubscribers(EventType.onConsoleMessage, mapData);
				return IndependentBrowser.this.onConsoleMessage(browser, level, message, source, line);
			}

			@Override
			public void onFullscreenModeChange(CefBrowser browser, boolean fullscreen) {
				if (fullscreen) {
					notifySubscribers(EventType.onFullScreenEntered);
				} else {
					notifySubscribers(EventType.onFullScreenExited);
				}
			}
		});
		clientHandler.addLoadHandler(new CefLoadHandler() {
			
			@Override
			public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
				notifySubscribers(EventType.onLoadingStateChange);
			}
			
			@Override
			public void onLoadStart(CefBrowser browser, CefFrame frame, TransitionType transitionType) {
				notifySubscribers(EventType.onLoadStart);
			}
			
			@Override
			public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText,
					String failedUrl) {
				notifySubscribers(EventType.onLoadError);
			}
			
			@Override
			public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
				CefFrame parentFrame = frame.getParent();
				final boolean isMain = frame.isMain();
				final long parentId = parentFrame != null ? parentFrame.getIdentifier() : 0;
				frame.getSource(new CefStringVisitor() {
					@Override
					public void visit(String source) {
						String jsonSource = source.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n");
						Map<String, Object> mapData = new HashMap<>();
						mapData.put("isMain", isMain);
						mapData.put("html", jsonSource);
						mapData.put("name", frame.getName());
						mapData.put("id", frame.getIdentifier());
						mapData.put("parentId", parentId);
						notifySubscribers(EventType.onLoadEnd, mapData);
					}
				});
			}
		});
		clientHandler.addFindHandler(new CefFindHandler() {
			@Override
			public void onFindResult(CefBrowser browser, int identifier, int count, Rectangle selectionRect,
					int activeMatchOrdinal, boolean finalUpdate) {
				if (finalUpdate) {
					Map<String, Object> mapData = new HashMap<>();
					mapData.put("count", count);
					mapData.put("activeMatchOrdinal", activeMatchOrdinal);
					notifySubscribers(EventType.onFindResult, mapData);
				}
			}
		});
		clientHandler.addPrintHandler(new  CefPrintHandlerAdapter() { });
		CommunicationManager commManager = CommunicationManager.get();
		if (commManager != null) {
			CefMessageRouter commRouter = CommRouterHandler.createRouter();
			commRouter.addHandler(CommRouterHandler.getInstance(commManager), true);
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

	protected void notifySubscribers(EventType eventType) {
		notifySubscribers(eventType, new HashMap<String, Object>());
	}

	protected synchronized void notifySubscribers(EventType eventType, Map<String, Object> mapData) {
		subscribeEvents.computeIfAbsent(eventType, m -> new ArrayList<EventAction>()).forEach(e -> {
			e.setJsonData(mapData);
			CompletableFuture.runAsync(e);
		});
	}
	
	public static void subscribeOnAfterCreated(EventAction eventAction) {
		eventActionOfAfterCreated = eventAction;
	}

	public synchronized long subscribe(EventType eventType, EventAction action) {
		created.thenRun(() -> {
			subscribeEvents.computeIfAbsent(eventType, m -> new ArrayList<EventAction>()).add(action);
			subscribeIndex.put(eventId, new ActionData(eventType, action));
		});
		return eventId++;
	}

	public synchronized boolean unSubscribe(long idEvent) {
		created.thenRun(() -> {
			ActionData actionData = subscribeIndex.get(idEvent);
			if (actionData != null) {
				subscribeEvents.get(actionData.eventType).remove(actionData.action);
				subscribeIndex.remove(idEvent);
			}
		});
		return false;
	}

	public synchronized void unSubscribeAll() {
		subscribeIndex.clear();
		subscribeEvents.clear();
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
		notifySubscribers(EventType.onZoomChanged);
	}

	@Override
	public double getZoom() {
		return getBrowser().getZoomLevel();
	}

	@Override
	public void executeJavacript(String script) {
		created.thenRun(() -> {
			getBrowser().executeJavaScript(script, "", 0);
		});
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
		jsonMessage.put("id", 0);
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
		CefBrowser browser = getBrowser();
		if (browser != null) {
			browser.setCloseAllowed();
			browser.setReference(null);
			browser.close(true);
			CefClient client = browser.getClient();
			if ( client != null) {
				client.dispose();
			}
		}
		unSubscribeAll();
		executor.shutdown();
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
				public void onDevToolsMethodResult(CefBrowser cefBrowser, int messageId, boolean success, String result,
						int resultSize) {
					try {
						debugPrint("onDevToolsMethodResult: " +result);
						if (id != messageId) {
							return;
						}
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
						executor.submit(() -> callback.success(result.apply(request)));
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
			if (settings.paper_width >= 1000) {
				pdfPrintSettings.paper_width = settings.paper_width;
			}
			if (settings.paper_height >= 1000) {
				pdfPrintSettings.paper_height = settings.paper_height;
			}
			pdfPrintSettings.prefer_css_page_size = settings.prefer_css_page_size;
			pdfPrintSettings.scale = settings.scale;
			pdfPrintSettings.margin_top = settings.margin_top;
			pdfPrintSettings.margin_right = settings.margin_right;
			pdfPrintSettings.margin_bottom = settings.margin_bottom;
			pdfPrintSettings.margin_left = settings.margin_left;
			if (settings.margin_type != null) {
				pdfPrintSettings.margin_type = CefPdfPrintSettings.MarginType.valueOf(settings.margin_type.name());;
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
}