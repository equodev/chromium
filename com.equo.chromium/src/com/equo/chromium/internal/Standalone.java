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


package com.equo.chromium.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserStandalone;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.misc.Rectangle;

public final class Standalone extends IndependentBrowser {
	private static ExecutorService executor = Executors.newCachedThreadPool();

	private void init(String url) {
		Engine.initCEF(true);
		createClient();
		setBrowser(getClientHandler().createBrowser(url, false, false, createRequestContext()));
	}

	public Standalone(String url) {
		init(url);
		getBrowser().createImmediately();
	}

	public Standalone(String url, Rectangle window) {
		init(url);
		CefBrowser browser = getBrowser();
		if (browser instanceof CefBrowserStandalone) {
			((CefBrowserStandalone) browser).setWindow(window);
		}
		browser.createImmediately();
	}

	public void addMessageRoute(String queryFunctionName, String cancelQueryFunctionName,
			Function<String, String> result) {
		getCreated().thenRun(() -> {
			CefMessageRouterConfig config = new CefMessageRouterConfig(queryFunctionName, cancelQueryFunctionName);
			CefMessageRouter messageRouter_ = CefMessageRouter.create(config);
			CefMessageRouterHandler newHandler = new CefMessageRouterHandlerAdapter() {
				@Override
				public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
						CefQueryCallback callback) {
					try {
						executor.submit(() -> callback.success(result.apply(request)));
						return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
					return false;
				}

			};
			messageRouter_.addHandler(newHandler, false);
			getClientHandler().addMessageRouter(messageRouter_);
		});
	}

	@Override
	public Object evaluate(String script) {
		throw new UnsupportedOperationException();
	}
	
}