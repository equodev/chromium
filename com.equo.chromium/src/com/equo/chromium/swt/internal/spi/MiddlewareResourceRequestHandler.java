package com.equo.chromium.swt.internal.spi;

import java.net.URI;
import java.util.List;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.network.CefRequest;

public class MiddlewareResourceRequestHandler extends CefResourceRequestHandlerAdapter {

	private SchemeHandlerManager schemeHandlerManager;
	List<SchemeDomainPair> registeredSchemeData;

	public MiddlewareResourceRequestHandler(SchemeHandlerManager schemeHandlerManager,
			List<SchemeDomainPair> registeredSchemeData) {
		this.schemeHandlerManager = schemeHandlerManager;
		this.registeredSchemeData = registeredSchemeData;
	}

	public boolean shouldHandleRequest(String url) {
		try {
			return schemeHandlerManager.shouldHandleRequest(url);
		} catch (AbstractMethodError e) {
		}
		if (registeredSchemeData != null) {
			for (SchemeDomainPair schemeDomain : registeredSchemeData) {
				String scheme = schemeDomain.getScheme();
				if (url.startsWith(scheme)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
		try {
			URI requestUri = URI.create(request.getURL());
			final RequestFilter requestFilter = schemeHandlerManager.getRequestFilter(requestUri.getScheme(),
					requestUri.getAuthority());
			if (requestFilter != null) {
				SchemeHandlerRequest requestToFilter = new SchemeHandlerRequest(request, frame);
				requestFilter.filterRequest(requestToFilter);
				request.setURL(requestToFilter.getURL());
				request.setMethod(requestToFilter.getMethod());
				request.setHeaderMap(requestToFilter.getHeaders());
			}
		} catch (NoSuchMethodError | AbstractMethodError t) {
		}
		return false;
	}
}
