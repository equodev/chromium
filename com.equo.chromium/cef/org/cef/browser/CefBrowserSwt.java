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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import org.cef.CefClient;
import org.cef.handler.CefWindowHandler;
import org.cef.handler.CefWindowHandlerAdapter;
import org.cef.misc.Point;
import org.cef.misc.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Widget;

public class CefBrowserSwt extends CefBrowser_N {
    private static Method autoScaleUp;
    private long handle;
    private Composite composite;
    protected org.eclipse.swt.graphics.Rectangle currentSize;

    static {
        try {
            Class<?> dpiClass = Class.forName("org.eclipse.swt.internal.DPIUtil");
            getAutoScaleMethod(dpiClass, "autoScaleUpUsingNativeDPI");
            if (!"gtk".equals(SWT.getPlatform()) || autoScaleUp == null) {
                getAutoScaleMethod(dpiClass, "autoScaleUp");
            } 
        } catch (ClassNotFoundException e) {
        }
    }

    private static void getAutoScaleMethod(Class<?> dpiClass, String method) {
        try {
            autoScaleUp = dpiClass.getDeclaredMethod(method, int.class);
        } catch (NoSuchMethodException e1) {
        }
    }

    private CefWindowHandler windowHandler = new CefWindowHandlerAdapter() {
        public Rectangle getRect(CefBrowser browser) {
            Rectangle rectangle = new Rectangle(0, 0, 0, 0);
            if (composite != null) {
                composite.getDisplay().syncExec(() -> {
                    Point size = getChromiumSize();
                    rectangle.setBounds(0, 0, size.x, size.y);
                    setCurrentSize();
                });
            }
            return rectangle;
        };
    };

    public CefBrowserSwt(CefClient client, String url, CefRequestContext context) {
        super(client, url, context, null, null);
    }

    public CefBrowserSwt(CefClient client, String url, CefRequestContext context, CefBrowser_N parent,
        Point inspectAt) {
        super(client, url, context, parent, inspectAt);
    }

    @Override
    public CompletableFuture<Object> createScreenshot(boolean nativeResolution) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public void createImmediately() {
        createBrowserIfRequired(false);
    }

    public void createImmediately(Composite composite) {
        this.composite = composite;
        this.handle = getHandle(composite);
        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                close(true);
            }
        });
        composite.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                if (!isClosed()) {
                    Point size = getChromiumSize();
                    setCurrentSize();
                    wasResized(size.x, size.y);
                }
            }
        });
        createImmediately();
    }

    public void resize() {
        if (!isClosed()) {
            Point size = getChromiumSize();
            if ("win32".equals(SWT.getPlatform())) {
                Monitor primaryMonitor = Display.getDefault().getPrimaryMonitor();
                Monitor currentMonitor = composite.getShell().getMonitor();
                if (!primaryMonitor.equals(currentMonitor)) {
                    wasResized(size.x + 1, size.y);
                }
            }

            setCurrentSize();
            wasResized(size.x, size.y);
        }
    }

    private long getHandle(Composite control) {
        long hwnd = 0;
        String platform = SWT.getPlatform();
        if ("cocoa".equals(platform)) {
            try {
                Field field = Control.class.getDeclaredField("view");
                field.setAccessible(true);
                Object nsview = field.get(control);

                Class<?> idClass = Class.forName("org.eclipse.swt.internal.cocoa.id");
                Field idField = idClass.getField("id");

                hwnd = (long /*int*/) idField.get(nsview);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("win32".equals(platform)) {
            try {
                Field field = Control.class.getDeclaredField("handle");
                field.setAccessible(true);
                hwnd = ((Number) field.get(control)).longValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                Field field = Widget.class.getDeclaredField("handle");
                field.setAccessible(true);
                hwnd = ((Number) field.get(control)).longValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return hwnd;
    }

    private boolean createBrowserIfRequired(boolean hasParent) {
        if (isClosed()) return false;

        long windowHandle = handle;
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
        return null;
    }

    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url,
            CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        return new CefBrowserSwt(client, url, context, parent, inspectAt);
    }

    @Override
    public CefWindowHandler getWindowHandler() {
        return windowHandler;
    }

    private Point getChromiumSize() {
        Point size = new Point(getComposite().getSize().x, getComposite().getSize().y);
        if ("cocoa".equals(SWT.getPlatform())) {
            return size;
        }
        if (autoScaleUp != null) {
            try {
                Point scaled = new Point((int)autoScaleUp.invoke(null, size.x), (int)autoScaleUp.invoke(null, size.y));
                if (scaled.x > size.x || scaled.y > size.y)
                    size = scaled;
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
            }
        }
        return size;
    }

    public org.eclipse.swt.graphics.Rectangle getCurrentBounds() {
        return currentSize;
    }

    private org.eclipse.swt.graphics.Rectangle setCurrentSize() {
        return currentSize = composite.getDisplay().map(
                       composite, composite.getShell(), composite.getClientArea());
    }

    public Composite getComposite() {
        return composite;
    }
}
