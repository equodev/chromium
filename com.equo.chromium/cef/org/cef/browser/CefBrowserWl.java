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


package org.cef.browser;

import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefRenderHandlerAdapter;
import org.cef.handler.CefScreenInfo;
import org.cef.misc.Point;
import org.cef.misc.Rectangle;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class CefBrowserWl extends CefBrowser_N implements CefRenderHandler {
    private boolean hasFocus;

    private CefRenderHandler renderHandler = new CefRenderHandlerAdapter() {
        public Rectangle getViewRect(CefBrowser browser) {
            return new Rectangle(0, 0, 1366, 768);
        };
    };

    public CefBrowserWl(CefClient client, String url, CefRequestContext context) {
        super(client, url, context, null, null);
    }

    @Override
    public CompletableFuture<Object> createScreenshot(boolean nativeResolution) {
        throw new UnsupportedOperationException("Unsupported");
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
                createBrowser(getClient(), windowHandle, getUrl(), true, false, null,
                        getRequestContext());
                return true;
            }
        }

        return false;
    }

    @Override
    public <T> T getUIComponent() {
        return null;
    }

    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url,
            CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        return null;
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return renderHandler;
    }

    @Override
    public void setFocus(boolean enable) {
        if (hasFocus == enable) return;
        hasFocus = enable;
        super.setFocus(enable);
    }
    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        return new Rectangle(0, 0, 1366, 768);
    };

    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        return false;
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        return viewPoint;
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {}

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {}

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects,
            ByteBuffer buffer, int width, int height) {}

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        return false;
    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {}
}
