package com.equo.chromium.internal;

import org.cef.browser.CefBrowser;

public class PopupBrowser extends IndependentBrowser {
    
    public PopupBrowser (CefBrowser browser) {
        setBrowser(browser);
        getBrowser().setReference(this);
    }

    @Override
    public Object getUIComponent() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean close() {
        getBrowser().setCloseAllowed();
        getBrowser().close(true);
        unSubscribeAll();
        return true;
    }
    
    @Override
    public void showDevTools() {
        throw new UnsupportedOperationException();
    }
}