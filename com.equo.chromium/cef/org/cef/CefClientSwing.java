// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserFactory;
import org.cef.browser.CefRequestContext;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Client that owns a browser and renderer.
 */
public class CefClientSwing extends CefClient {
    private volatile CefBrowser focusedBrowser_ = null;
    private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (focusedBrowser_ != null) {
                Component browserUI = focusedBrowser_.getUIComponent();
                Object oldUI = evt.getOldValue();
                if (isPartOf(oldUI, browserUI)) {
                    focusedBrowser_.setFocus(false);
                    focusedBrowser_ = null;
                }
            }
        }
    };

    /**
     * The CTOR is only accessible within this package.
     * Use CefApp.createClient() to create an instance of
     * this class.
     * @see org.cef.CefApp.createClient()
     */
    CefClientSwing() throws UnsatisfiedLinkError {
        super();

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(
                propertyChangeListener);
    }

    private boolean isPartOf(Object obj, Component browserUI) {
        if (obj == browserUI) return true;
        if (obj instanceof Container) {
            Component childs[] = ((Container) obj).getComponents();
            for (Component child : childs) {
                return isPartOf(child, browserUI);
            }
        }
        return false;
    }

    @Override
    public CefBrowser createBrowser(String url, boolean isOffscreenRendered, boolean isTransparent,
            CefRequestContext context, CefBrowserSettings settings) {
        if (isDisposed_)
            throw new IllegalStateException("Can't create browser. CefClient is disposed");
        return CefBrowserFactory.create(this, url, isOffscreenRendered, isTransparent, context, settings);
    }

    @Override
    public void onTakeFocus(CefBrowser browser, boolean next) {
        super.onTakeFocus(browser, next);
        Component component = browser.getUIComponent();
        Container parent = component.getParent();
        if (parent != null) {
            FocusTraversalPolicy policy = null;
            while (parent != null) {
                policy = parent.getFocusTraversalPolicy();
                if (policy != null) break;
                parent = parent.getParent();
            }
            if (policy != null) {
                Component nextComp = next
                        ? policy.getComponentAfter(parent, browser.getUIComponent())
                        : policy.getComponentBefore(parent, browser.getUIComponent());
                if (nextComp == null) {
                    policy.getDefaultComponent(parent).requestFocus();
                } else {
                    nextComp.requestFocus();
                }
            }
        }
        focusedBrowser_ = null;
    }

    @Override
    public void onGotFocus(CefBrowser browser) {
        super.onGotFocus(browser);

        focusedBrowser_ = browser;
    }

    @Override
    public void onBeforeClose(CefBrowser browser) {
        super.onBeforeClose(browser);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(
                propertyChangeListener);
    }
}
