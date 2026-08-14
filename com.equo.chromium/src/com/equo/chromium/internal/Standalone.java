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
package com.equo.chromium.internal;

import java.util.Vector;

import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserStandalone;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefFileDialogCallback;
import org.cef.handler.CefDialogHandler;
import org.cef.handler.CefDownloadHandlerAdapter;
import org.cef.misc.Rectangle;

import com.equo.chromium.ChromiumBrowserStandalone;


public final class Standalone extends IndependentBrowser implements ChromiumBrowserStandalone{

	public Standalone(String url, String title, Rectangle window) {
		Engine.initCEF(Engine.BrowserType.STANDALONE);
		createClient();

		setBrowser(getClientHandler().createBrowser(url, false, false, createRequestContext(null), null));
		getBrowser().setReference(this);

		CefBrowser browser = getBrowser();
		if (browser instanceof CefBrowserStandalone) {
			CefBrowserStandalone standaloneBrowser = (CefBrowserStandalone) browser;
			if (title != null && !title.trim().isEmpty()) {
				standaloneBrowser.createBrowserTitle(title);
			}
			if (window != null) {
				standaloneBrowser.setWindow(window);
			}
		}
		getClientHandler().addDownloadHandler(new CefDownloadHandlerAdapter() {
			@Override
			public boolean onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem, String suggestedName,
					CefBeforeDownloadCallback callback) {
				callback.Continue("", true);
				return true;
			}
		});

		getClientHandler().addDialogHandler(new CefDialogHandler() {
			@Override
			public boolean onFileDialog(CefBrowser browser, FileDialogMode mode, String title, String defaultFilePath,
					Vector<String> acceptFilters, Vector<String> acceptExtensions, Vector<String> acceptDescriptions,
					CefFileDialogCallback callback) {
				return false;
			}
		});
		browser.createImmediately();
	}

	public Standalone(String url) {
		this(url, null, null);
	}

	public Standalone(String url, Rectangle window) {
		this(url, null, window);
	}

	public Standalone(String url, String title) {
		this(url, title, null);
	}

	@Override
	public boolean close() {
		super.close();
		if (!isCreated().isDone() && !Boolean.getBoolean("chromium.manual_stop_browsers")) {
			ChromiumBrowserStandalone.stopBrowsers();
		}
		return true;
	}

	@Override
	public void setFullscreen(boolean fullscreen) {
		isCreated().thenRun(() -> {
			CefBrowser browser = getBrowser();
			if (browser instanceof CefBrowserStandalone) {
				((CefBrowserStandalone) browser).setFullscreen(fullscreen);
			}
		});
	}

	@Override
	public boolean isFullscreen() {
		CefBrowser browser = getBrowser();
		return browser instanceof CefBrowserStandalone && ((CefBrowserStandalone) browser).isFullscreen();
	}

	@Override
	public void setWindowIcon(String iconPath) {
		if (!OS.isWindows())
			throw new UnsupportedOperationException("Window icon customization not supported on Linux or MacOS");
		isCreated().thenRun(() -> {
			CefBrowser browser = getBrowser();
			if (browser instanceof CefBrowserStandalone) {
				((CefBrowserStandalone) browser).setWindowIcon(iconPath);
			}
		});
	}

	@Override
	public void setWindowTitle(String title) {
		isCreated().thenRun(() -> {
			CefBrowser browser = getBrowser();
			if (browser instanceof CefBrowserStandalone) {
				((CefBrowserStandalone) browser).setWindowTitle(title);
			}
		});
	}

	@Override
	public void setWindowSize(int width, int height) {
		isCreated().thenRun(() -> {
			CefBrowser browser = getBrowser();
			if (browser instanceof CefBrowserStandalone) {
				((CefBrowserStandalone) browser).setWindowSize(width, height, true);
			}
		});
	}

	@Override
	public void restoreWindow() {
		isCreated().thenRun(() -> {
			CefBrowser browser = getBrowser();
			if (browser instanceof CefBrowserStandalone) {
				((CefBrowserStandalone) browser).restore();
			}
		});
	}

	@Override
	public void maximizeWindow() {
		isCreated().thenRun(() -> {
			CefBrowser browser = getBrowser();
			if (browser instanceof CefBrowserStandalone) {
				((CefBrowserStandalone) browser).maximize();
			}
		});
	}

	@Override
	public void minimizeWindow() {
		isCreated().thenRun(() -> {
			CefBrowser browser = getBrowser();
			if (browser instanceof CefBrowserStandalone) {
				((CefBrowserStandalone) browser).minimize();
			}
		});
	}

	@Override
	public void setWindowBounds(int x, int y, int width, int height) {
		isCreated().thenRun(() -> {
			CefBrowser browser = getBrowser();
			if (browser instanceof CefBrowserStandalone) {
				((CefBrowserStandalone) browser).setWindowBounds(x, y, width, height);
			}
		});
	}

	@Override
	public void setWindowPosition(int x, int y) {
		isCreated().thenRun(() -> {
			CefBrowser browser = getBrowser();
			if (browser instanceof CefBrowserStandalone) {
				((CefBrowserStandalone) browser).setWindowPosition(x, y);
			}
		});
	}
}
