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

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserStandalone;
import org.cef.misc.Rectangle;

public final class Standalone extends IndependentBrowser {

	private void init(String url) {
		Engine.initCEF(Engine.BrowserType.STANDALONE);
		createClient();
		setBrowser(getClientHandler().createBrowser(url, false, false, createRequestContext()));
		getBrowser().setReference(this);
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

}