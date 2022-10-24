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
import org.cef.misc.Point;

import java.util.concurrent.CompletableFuture;

public class CefPopup extends CefBrowser_N {
    public CefPopup(CefBrowser parent) {
        super(null, null, null, (CefBrowser_N) parent, null);
    }

    @Override
    public void createImmediately() {}

    @Override
    public <T> T getUIComponent() {
        return null;
    }

    @Override
    public CompletableFuture<Object> createScreenshot(boolean nativeResolution) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url,
            CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        return null;
    }

    public CefBrowser getParent() {
        CefBrowser_N p = getParentBrowser();
        if (p.isPopup()) {
            return p.getParentBrowser();
        }
        return p;
    }

    @Override
    public synchronized boolean doClose() {
        super.close(true);
        return super.doClose();
    }

    @Override
    public synchronized void onBeforeClose() {
        parent_ = null;
        super.onBeforeClose();
    }
}