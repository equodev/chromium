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
package org.cef.browser;

import java.util.concurrent.CompletableFuture;

import org.cef.CefClient;
import org.cef.handler.CefClientHandler;
import org.cef.handler.CefWindowHandler;
import org.cef.handler.CefWindowHandlerAdapter;
import org.cef.misc.Point;
import org.cef.misc.Rectangle;

public class CefBrowserStandalone extends CefBrowser_N {
    private Rectangle window = null;
    private String windowTitle = "";
    private boolean frameless = false;
    private boolean isFullscreenMode = false;
    private CefWindowHandler windowHandler = new CefWindowHandlerAdapter() {
        public Rectangle getRect(CefBrowser browser) {
            if (window == null) {
                return new Rectangle(0, 0, 800, 600);
            }
            return window;
        };
    };

    public CefBrowserStandalone(CefClient client, String url, CefRequestContext context) {
        super(client, url, context, null, null, null);
    }

    public CefBrowserStandalone(CefClient client, String url, CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
		super(client, url, context, parent, inspectAt, null);
	}

    @Override
    public CompletableFuture<Object> createScreenshot(boolean nativeResolution) {
        throw new UnsupportedOperationException("Unsupported");
    }

    public Rectangle getWindow() {
        return window;
    }

    public void setWindow(Rectangle window) {
        this.window = window;
    }

    public void createBrowserTitle(String title) {
        this.windowTitle = title != null ? title : "";
    }
    
    public String getWindowTitle() {
        return windowTitle;
    }

    public void setFrameless(boolean frameless) {
        this.frameless = frameless;
    }

    @Override
    public boolean isFramelessWindow() {
        return frameless;
    }

    public void setFullscreen(boolean fullscreen) {
        if (isFullscreen() == fullscreen) return;
        
        this.isFullscreenMode = fullscreen;
        super.setFullscreen(fullscreen);
    }

    public boolean isFullscreen() {
        return isFullscreenMode;
    }

    @Override
    protected void createBrowser(CefClientHandler clientHandler, long windowHandle, String url,
            boolean osr, boolean transparent, Object canvas, CefRequestContext context) {
        super.createBrowser(clientHandler, windowHandle, url, osr, transparent, canvas, context, windowTitle);
    }

    @Override
    public void createImmediately() {
        createBrowserIfRequired(false);
    }

    @Override
    public synchronized void onBeforeClose() {
        super.onBeforeClose();
    }

    private boolean createBrowserIfRequired(boolean hasParent) {
        if (isClosed()) return false;

        long windowHandle = 0;
        if (getNativeRef("CefBrowser") == 0) {
            if (getParentBrowser() != null) {
                createDevTools(getParentBrowser(), getClient(), windowHandle, false, false, null,
                        getInspectAt());
                return true;
            } else {
                createBrowser(getClient(), windowHandle, getUrl(), false, false, null,
                        getRequestContext());
                return true;
            }
        }

        return false;
    }

    @Override
    public <T> T getUIComponent() {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url,
            CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        return new CefBrowserStandalone(client, url, context, this, inspectAt);
    }

    @Override
    public CefWindowHandler getWindowHandler() {
        return windowHandler;
    }
}
