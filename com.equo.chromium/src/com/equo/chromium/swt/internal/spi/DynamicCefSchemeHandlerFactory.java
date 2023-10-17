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
