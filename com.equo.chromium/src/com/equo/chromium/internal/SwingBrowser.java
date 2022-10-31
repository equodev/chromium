package com.equo.chromium.internal;

import java.awt.Container;

import javax.swing.JPanel;

import org.cef.browser.CefBrowser;
import org.cef.handler.CefFocusHandlerAdapter;

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
		});
		setBrowser(getClientHandler().createBrowser(url, false, false));
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
}
