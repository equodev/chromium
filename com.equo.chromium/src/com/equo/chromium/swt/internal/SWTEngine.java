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
package com.equo.chromium.swt.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefClientSwt;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserSwt;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import com.equo.chromium.ChromiumBrowser;
import com.equo.chromium.internal.Utils;

public class SWTEngine {

	
	public static void initCef(AtomicBoolean closing, AtomicBoolean shuttingDown, Runnable shutdownRunnable) {
		Display.getDefault().asyncExec(() -> {
			Display.getDefault().addListener(SWT.Close, e -> {
				closing.set(true);
			});
			Display.getDefault().disposeExec(() -> {
				CefApp.getInstance().doMessageLoopWork(-1);
				if (shuttingDown.get()) {
					// already shutdown
					return;
				}
				if (!Utils.isMac() || !closing.get()) {
					shuttingDown.set(true);
					if (!Boolean.getBoolean("chromium.disable_close_windowless_before_dispose")) {
						closeWindowlessBrowsers();
					}
					shutdownRunnable.run();
				}
			});
		});
	}

	private static void closeWindowlessBrowsers() throws UnsatisfiedLinkError {
		Set<CefClient> clients = CefApp.getInstance().getAllClients();
		for (CefClient client : clients) {
			if (client instanceof CefClientSwt && ((CefClientSwt) client).isOffscreenRendered()) {
				Object[] browsers = client.getAllBrowser();
				if (browsers.length > 0) {
					for (Object c : browsers) {
						ChromiumBrowser browserReference = (ChromiumBrowser) ((CefBrowser) c).getReference();
						if (browserReference != null) {
							browserReference.close();
						}
					}
				}
			}
		}
		awaitDisposeWindowless();
	}

	private static void awaitDisposeWindowless() {
		long endTime = System.currentTimeMillis() + 3000;
		boolean continueLoop = true;
		while (continueLoop && (System.currentTimeMillis() < endTime)) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			CefApp.getInstance().doMessageLoopWork(-1);
			int clientSize = CefApp.getInstance().getClientsSize();
			if (clientSize == 0
					|| (clientSize == 1 && !((CefClientSwt) (CefApp.getInstance().getAllClients().iterator().next()))
							.isOffscreenRendered())) {
				continueLoop = false;
			}
		}
	}

	public static void onContextInitialized(CefApp app) {
		registerBrowserFunctions(app);
	}

	public static void registerBrowserFunctions(CefApp app) {
		app.registerSchemeHandlerFactory("https", "functions", new CefSchemeHandlerFactory() {
			@Override
			public CefResourceHandler create(CefBrowser browser, CefFrame frame,
					String schemeName, CefRequest request) {
				if (!browser.isPopup()) {
					if (isPartial(request))
						return Chromium.getChromium(browser).functionsResourceHandler.peek();
					return Chromium.getChromium(browser).createFunctionResourceHandler();
				}
				return null;
			}
			
			protected boolean isPartial(CefRequest request) {
				try {
					URI url = new URI(request.getURL());
					return url.getFragment() != null;
				} catch (URISyntaxException e) {
					return false;
				}
			}
		});
	}

	public static boolean isSystemDarkTheme() {
		String swtDark = System.getProperty("chromium.swt_dark");
		if (swtDark == null) {
			boolean isDarkTheme = false;
			try {
				Class<?> systemThemeClass = Class.forName("org.eclipse.swt.widgets.Display");
				// Method available since SWT 3.112
				Method isSystemDarkThemeMethod = systemThemeClass.getMethod("isSystemDarkTheme");
				if (isSystemDarkThemeMethod != null) {
					isDarkTheme = (boolean) isSystemDarkThemeMethod.invoke(null);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException
					| NoSuchMethodException | SecurityException e) {
			}
			return isDarkTheme;
		} else if (Boolean.parseBoolean(swtDark)) {
			return true;
		} else {
			return false;
		}
	}

	public static int getSWTVersion() {
		return SWT.getVersion();
	}

	public static int getScale() {
		if (CefBrowserSwt.autoScaleUp != null) {
			try {
				return (int) CefBrowserSwt.autoScaleUp.invoke(null, 1);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			}
		}
		return 1;
	}
}
