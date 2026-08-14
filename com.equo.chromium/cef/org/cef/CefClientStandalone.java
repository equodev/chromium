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
package org.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserStandalone;
import org.cef.browser.CefBrowserWl;
import org.cef.browser.CefRequestContext;

public class CefClientStandalone extends CefClient {
    private boolean creating;

    @Override
    public CefBrowser createBrowser(String url, boolean isOffscreenRendered, boolean isTransparent,
            CefRequestContext context, CefBrowserSettings settings) {
        creating = true;
        if (isOffscreenRendered) {
            return new CefBrowserWl(this, url, context);
        }
        return new CefBrowserStandalone(this, url, context);
    }

    @Override
    public void onBeforeClose(CefBrowser browser) {
        CefApp app = CefApp.getInstance();
        boolean otherClientsExist = app.getAllClients().size() > 1;
        super.onBeforeClose(browser);
        if (otherClientsExist) {
            if (!isDisposed() && getAllBrowser().length == 0 && creating) {
                dispose();
            }
            return;
        }
        if (!Boolean.valueOf(System.getProperty("chromium.manual_stop_browsers", "true"))) {
            CefAppStandalone.runOnUIThread(() -> {
                if (!Boolean.getBoolean("chromium.multi_threaded_message_loop")) {
                    app.shutdown();
                } else {
                    app.quitMessageLoop();
                    app.shutdown();
                }
            });
        } else {
            if (OS.isMacintosh()) app.requestShutdownAfterMessageLoop();
            app.quitMessageLoop();
            if (Boolean.getBoolean("chromium.multi_threaded_message_loop")) {
                CefAppStandalone.runOnUIThread(() -> {
                    app.shutdown();
                });
            }
        }
    }

    public boolean isDisposed() {
        return isDisposed_;
    }
}