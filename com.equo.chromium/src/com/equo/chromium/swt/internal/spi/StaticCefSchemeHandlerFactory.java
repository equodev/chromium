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
