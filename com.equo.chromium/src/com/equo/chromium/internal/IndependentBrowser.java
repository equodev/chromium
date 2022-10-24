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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.cef.CefClient;
import org.cef.CefSettings.LogSeverity;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefCallback;
import org.cef.callback.CefCompletionCallback;
import org.cef.callback.CefDevToolsMessageObserverAdapter;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler.ErrorCode;
import org.cef.handler.CefRequestHandlerAdapter;

import com.equo.chromium.ChromiumBrowser;
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

	protected CompletableFuture<Boolean> getCreated() {
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
				created.complete(true);
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
				return IndependentBrowser.this.onConsoleMessage(browser, level, message, source, line);
			}
		});
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
					System.setProperty("chromium.ssl.cert", new String(derpemBytes , "ASCII").replaceAll("\\r\\n", "\n"));
				} catch (IOException e) {
					debugPrint("Failed to read file "+derpem);
					e.printStackTrace();
				}
			} else {
				debugPrint("Cannot read file '"+derpem+"', trying as string");
				System.setProperty("chromium.ssl.cert", derpem.replaceAll("\\r\\n", "\n"));
			}
			return true;
		}
		return false;
	}

	private void debugPrint(String log) {
		Engine.debug(log, getBrowser());
	}

	protected boolean onConsoleMessage(CefBrowser browser, LogSeverity level, String message, String source,
			int line) {
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
	public void find(String search, boolean forward, boolean matchCase) {
		boolean findNext = Objects.equals(search, lastSearch);
		lastSearch = search;
		if (search == null || search.isEmpty()) {
			getBrowser().stopFinding(true);
		} else {
			getBrowser().find(search, forward, matchCase, findNext);
		}
	}

	@Override
	public void zoom(double zoomLevel) {
		getBrowser().setZoomLevel(zoomLevel);
	}
	
	@Override
	public void executeJavacript(String script) {
		getBrowser().executeJavaScript(script, "", 0);
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
		return captureScreenshot(0,0,0,0,1);
	}

	@Override
	public CompletableFuture<byte[]> captureScreenshot(int x, int y, int width, int height, int scale) {
		CompletableFuture<byte[]> screenshotResult = new CompletableFuture<>();
		new CefDevToolsMessageObserverAdapter(getBrowser()) {
			@Override
			public void onDevToolsMethodResult(CefBrowser cefBrowser, int messageId, boolean success, String result,
					int resultSize) {
				try {
					JsonObject json = (JsonObject)Jsoner.deserialize(result);
					screenshotResult.complete((byte[])((String)json.getOrDefault("data", "")).getBytes(StandardCharsets.UTF_8));
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
			}
			else {
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
		getBrowser().setCloseAllowed();
		getBrowser().close(true);
		clientHandler.dispose();
		return true;
	}
}