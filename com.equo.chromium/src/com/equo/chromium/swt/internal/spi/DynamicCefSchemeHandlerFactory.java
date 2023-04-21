package com.equo.chromium.swt.internal.spi;

import java.net.URI;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

public class DynamicCefSchemeHandlerFactory implements CefSchemeHandlerFactory {

	private SchemeHandlerManager schemeHandlerManager;

	public DynamicCefSchemeHandlerFactory(SchemeHandlerManager schemeHandlerManager) {
		this.schemeHandlerManager = schemeHandlerManager;
	}

	@Override
	public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
		String TEXT_URL = System.getProperty("chromium.setTextAsUrl","");
		// Return null when request start with setTextAsUrl and not constains textPath (default)
		if (!TEXT_URL.isEmpty() && request.getURL().startsWith(TEXT_URL)) {
			// Popup dont work with data url
			if (browser.isPopup() || !"setText".equals(request.getHeaderByName("chromium"))) {
				return null;
			}
		}
		try {
			URI requestUri = URI.create(request.getURL());
			final SchemeHandler schemeHandler = schemeHandlerManager.getSchemeHandler(schemeName,
					requestUri.getAuthority());
			if (schemeHandler != null) {
				return new DelegatingCefResourceHandler(schemeHandler);
			}
			return null;
		} catch (Throwable t) {
			return null;
		}
	}

}
