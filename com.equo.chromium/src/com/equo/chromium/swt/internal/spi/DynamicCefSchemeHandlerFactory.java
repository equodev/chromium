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
		try {
			URI requestUri = URI.create(request.getURL());
			final SchemeHandler schemeHandler = schemeHandlerManager.getSchemeHandler(schemeName,
					requestUri.getAuthority());
			if (schemeHandler != null) {
				CefResourceHandler handler = new DelegatingCefResourceHandler(schemeHandler);
				if (handler.processRequest(request, null)) {
					return handler;
				}
			}
			return null;
		} catch (Throwable t) {
			return null;
		}
	}

}
