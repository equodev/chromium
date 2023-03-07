package com.equo.chromium.swt.internal.spi;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

public class StaticCefSchemeHandlerFactory implements CefSchemeHandlerFactory {

	private SchemeHandlerManager schemeHandlerManager;
	private SchemeDomainPair schemeData;
	private static String TEXT_URL = System.getProperty("chromium.setTextAsUrl","");

	public StaticCefSchemeHandlerFactory(SchemeHandlerManager schemeHandlerManager, SchemeDomainPair schemeData) {
		this.schemeHandlerManager = schemeHandlerManager;
		this.schemeData = schemeData;
	}

	@Override
	public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
		SchemeHandler schemeHandler = schemeHandlerManager.getSchemeHandler(schemeData.getScheme(),
				schemeData.getDomain());

		// Return null when request start with setTextAsUrl and not constains textPath (default)
		if (!TEXT_URL.isEmpty() && request.getURL().startsWith(TEXT_URL)) {
			// Popup dont work with data url
			if (browser.isPopup() || !"setText".equals(request.getHeaderByName("chromium"))) {
				return null;
			}
		}

		if (schemeHandler != null) {
			return new DelegatingCefResourceHandler(schemeHandler);
		}
		return null;
	}

}
