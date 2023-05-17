package com.equo.chromium.swt.internal.spi;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

public class StaticCefSchemeHandlerFactory implements CefSchemeHandlerFactory {

	private SchemeHandlerManager schemeHandlerManager;
	private SchemeDomainPair schemeData;

	public StaticCefSchemeHandlerFactory(SchemeHandlerManager schemeHandlerManager, SchemeDomainPair schemeData) {
		this.schemeHandlerManager = schemeHandlerManager;
		this.schemeData = schemeData;
	}

	@Override
	public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
		SchemeHandler schemeHandler = schemeHandlerManager.getSchemeHandler(schemeData.getScheme(),
				schemeData.getDomain());
		if (schemeHandler != null) {
			CefResourceHandler handler = new DelegatingCefResourceHandler(schemeHandler);
			if (handler.processRequest(request, null)) {
				return handler;
			}
		}
		return null;
	}

}
