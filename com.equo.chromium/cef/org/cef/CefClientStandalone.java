package org.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserStandalone;
import org.cef.browser.CefBrowserWl;
import org.cef.browser.CefRequestContext;

public class CefClientStandalone extends CefClient {
    private boolean creating;

    @Override
    public CefBrowser createBrowser(String url, boolean isOffscreenRendered, boolean isTransparent,
            CefRequestContext context) {
        creating = true;
        if (isOffscreenRendered) {
            return new CefBrowserWl(this, url, context);
        }
        return new CefBrowserStandalone(this, url, context);
    }

    @Override
    public void onBeforeClose(CefBrowser browser) {
        super.onBeforeClose(browser);
        if (CefApp.getInstance().getAllClients().size() > 1 || isDisposed_) {
            if (!isDisposed_ && getAllBrowser().length == 0 && creating) {
                dispose();
            }
            return;
        }
        CefApp.getInstance().quitMessageLoop();
    }

    public boolean isDisposed() {
        return isDisposed_;
    }
}