package org.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserStandalone;
import org.cef.browser.CefRequestContext;

public class CefClientStandalone extends CefClient {
    private boolean creating;

    @Override
    public CefBrowser createBrowser(String url, boolean isOffscreenRendered, boolean isTransparent,
            CefRequestContext context) {
        creating = true;
        return new CefBrowserStandalone(this, url, context);
    }

    @Override
    public void onBeforeClose(CefBrowser browser) {
        super.onBeforeClose(browser);
        if (!isDisposed_ && getAllBrowser().length == 0 && !creating) {
            dispose();
        }
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            System.exit(0);
        }).start();
    }

    public boolean isDisposed() {
        return isDisposed_;
    }
}