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

import java.lang.reflect.Method;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserWl;
import org.cef.misc.Rectangle;

import com.equo.chromium.ChromiumBrowser;
import com.equo.chromium.internal.Engine.BrowserType;

public final class Windowless extends IndependentBrowser {

	public Windowless(String url) {
		this(url, null);
	}

	public Windowless(String url, Rectangle window) {
		Engine.initCEF(getBrowserType());
		createClient();
		setBrowser(getClientHandler().createBrowser(url, true, false, createRequestContext()));
		CefBrowser browser = getBrowser();
		browser.setReference(this);
		browser.createImmediately();
		if (window != null && browser instanceof CefBrowserWl) {
			((CefBrowserWl) browser).setWindow(window);
		}
	}

	private static BrowserType getBrowserType() {
		if (Boolean.getBoolean("chromium.force_windowless_swt")) {
			return BrowserType.SWT;
		}
		if (Engine.browserTypeInitialized != null) {
			return null;
		}
		try {
			Class<?> clazz = Class.forName("org.eclipse.swt.widgets.Display", false,
					ChromiumBrowser.class.getClassLoader());
			if (clazz != null) {
				Method findDisplay = clazz.getDeclaredMethod("findDisplay", Thread.class);
				for (Thread thread : Thread.getAllStackTraces().keySet()) {
					if (findDisplay.invoke(null, thread) != null) {
						return BrowserType.SWT;
					}
				}
			}
		} catch (Throwable e) {

		}
		return BrowserType.STANDALONE;
	}
}