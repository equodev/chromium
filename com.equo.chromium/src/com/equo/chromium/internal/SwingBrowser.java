package com.equo.chromium.internal;

import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefFocusHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import com.equo.chromium.utils.EventType;

public class SwingBrowser extends IndependentBrowser {
	private boolean ignoreFirstFocus = true;

	private void init(String url) {
		Engine.initCEF(Engine.BrowserType.SWING);
		createClient();
		getClientHandler().addFocusHandler(new CefFocusHandlerAdapter() {
			@Override
			public boolean onSetFocus(CefBrowser browser, FocusSource source) {
				if (ignoreFirstFocus && source == FocusSource.FOCUS_SOURCE_NAVIGATION) {
					ignoreFirstFocus = false;
					return true;
				}
				return false;
			}

			@Override
			public void onGotFocus(CefBrowser browser) {
				if (OS.isWindows()) {
					KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
					browser.setFocus(true);
				}
			}
		});
		setBrowser(getClientHandler().createBrowser(url, false, false));
		getBrowser().setReference(this);
		createClipboardRouters();
	}

	@Override
	public boolean setUrl(String url) {
		ignoreFirstFocus = true;
		return super.setUrl(url);
	}

	public SwingBrowser(Object parent, String layout, String url) {
		init(url);
		((Container) parent).add((JPanel) getBrowser().getUIComponent(), layout);
	}

	public SwingBrowser(String url) {
		init(url);
		getBrowser().createImmediately();
	}

	@Override
	public Object evaluate(String script) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getUIComponent() {
		return getBrowser().getUIComponent();
	}

	private void createClipboardRouters() {
		CefMessageRouter writeText = CefMessageRouter
				.create(new CefMessageRouterConfig("__writeText", "__writeTextCancel"));
		CefMessageRouter readText = CefMessageRouter
				.create(new CefMessageRouterConfig("__readText", "__readTextCancel"));
		CefMessageRouterHandlerAdapter writeTextHandler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {
				SwingUtilities.invokeLater(() -> {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					try {
						if (request == null || request.isEmpty()) {
							cb.setContents(new StringSelection(""), null);
						} else {
							StringSelection transfer = new StringSelection(request);
							cb.setContents(transfer, null);
						}
						Map<String, Object> mapData = new HashMap<>();
						mapData.put("text", request);
						notifySubscribers(EventType.onClipboardWriteText, mapData);
						callback.success(request);
					} catch (Exception e) {
						e.printStackTrace();
						callback.failure(0, e.getMessage());
					}
				});
				return true;
			}
		};
		CefMessageRouterHandlerAdapter readTextHandler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {
				SwingUtilities.invokeLater(() -> {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					try {
						Transferable transferable = cb.getContents(null);
						String text = (String) transferable.getTransferData(DataFlavor.stringFlavor);
						Map<String, Object> mapData = new HashMap<>();
						mapData.put("text", text);
						notifySubscribers(EventType.onClipboardReadText, mapData);
						callback.success(text);
					} catch (Exception e) {
						e.printStackTrace();
						callback.failure(0, e.getMessage());
					}
				});
				return true;
			}
		};
		writeText.addHandler(writeTextHandler, true);
		readText.addHandler(readTextHandler, true);
		getBrowser().getClient().addMessageRouter(writeText);
		getBrowser().getClient().addMessageRouter(readText);
	};
}
