// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.WindowEvent;

import javax.swing.SwingUtilities;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.misc.Point;

/**
 * This class represents all methods which are connected to the
 * native counterpart CEF.
 * The visibility of this class is "package". To create a new
 * CefBrowser instance, please use CefBrowserFactory.
 */
public abstract class CefBrowserSwing extends CefBrowser_N {

    protected CefBrowserSwing(CefClient client, String url, CefRequestContext context,
            CefBrowser_N parent, Point inspectAt) {
        this(client, url, context, parent, inspectAt, null);
    }

    protected CefBrowserSwing(CefClient client, String url, CefRequestContext context,
            CefBrowser_N parent, Point inspectAt, CefBrowserSettings settings) {
        super(client, url, context, parent, inspectAt, settings);
    }

    @Override
    public synchronized boolean doClose() {
        if (!super.doClose()) {
            return false;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Trigger close of the parent window.
                Component parent = SwingUtilities.getRoot(getUIComponent());
                if (parent != null) {
                    parent.dispatchEvent(
                            new WindowEvent((Window) parent, WindowEvent.WINDOW_CLOSING));
                }
            }
        });

        // Cancel the close.
        return true;
    }

    @Override
    public void close(boolean force) {
        super.close(force);
    }
}
