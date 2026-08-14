/****************************************************************************
**
** Copyright (C) 2026 Equo
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
