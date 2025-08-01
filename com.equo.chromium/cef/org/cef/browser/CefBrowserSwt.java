/****************************************************************************
**
** Copyright (C) 2025 Equo
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

import static org.cef.CefColor.DARK_MODE;
import static org.cef.CefColor.DARK_MODE_COLOR;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.OS;
import org.cef.CefColor;
import org.cef.OS;
import org.cef.handler.CefWindowHandler;
import org.cef.handler.CefWindowHandlerAdapter;
import org.cef.misc.Point;
import org.cef.misc.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Widget;
import org.cef.misc.SWTUtils;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;

public class CefBrowserSwt extends CefBrowser_N {
    public static Method autoScaleUp;
    private long handle;
    private Composite composite;
    protected org.eclipse.swt.graphics.Rectangle currentSize;
    private boolean visibleWayland = true;
    private Color parentBackgroundColor = null;
    private Point chromiumSize = null;

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

    public int getBackgroundColor() {
        if (composite.isDisposed()) {
            return 0;
        }
        Color colorSwt = null;

        if (Boolean.getBoolean("chromium.inherit_bg_color")) {
            colorSwt = parentBackgroundColor;
        }

        if (colorSwt == null) {
            if (DARK_MODE) {
                colorSwt = new Color(composite.getDisplay(), DARK_MODE_COLOR.getRed(), DARK_MODE_COLOR.getGreen(),
                        DARK_MODE_COLOR.getBlue());
            } else {
                colorSwt = new Color(composite.getDisplay(), 255, 255, 255);
            }
        }

        return (int) new CefColor(colorSwt.getAlpha(),
                                      colorSwt.getRed(),
                                      colorSwt.getGreen(),
                                      colorSwt.getBlue()).getColor();
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
            if (composite != null && !composite.isDisposed()) {
                Point size = SWTUtils.IS_WIN_MULTITHREAD ? chromiumSize : SWTUtils.syncExec(() -> getChromiumSize());
                if (OS.isWayland()) {
                    composite.getDisplay().syncExec(() -> {
                        Point offset = getChromiumOffset();
                        rectangle.setBounds(offset.x, offset.y, size.x, size.y);
                    });
                } else {
                    rectangle.setBounds(0, 0, size.x, size.y);
                }
                SWTUtils.winMTExec(() -> {
                    if (!composite.isDisposed())
                         setCurrentSize();
                }, false);
            }
            return rectangle;
        };
    };

    public CefBrowserSwt(CefClient client, String url, CefRequestContext context) {
        super(client, url, context, null, null, null);
    }

    public CefBrowserSwt(CefClient client, String url, CefRequestContext context,
            CefBrowser_N parent, Point inspectAt) {
        super(client, url, context, parent, inspectAt, null);
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
        if (composite.getParent() != null) {
        	this.parentBackgroundColor = composite.getParent().getBackground();
        }
        this.handle = getHandle(composite);
        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                close(true);
                CefApp.getInstance().doMessageLoopWork(1);
            }
        });
        composite.addControlListener(new ControlAdapter() {
            @Override
            public void controlMoved(ControlEvent e) {
                if (OS.isWayland() && !isClosed()) {
                    Point size = getChromiumSize();
                    Point offset = getChromiumOffset();
                    updateUI(new Rectangle(offset.x, offset.y, size.x, size.y), null);
                }
            }
            @Override
            public void controlResized(ControlEvent e) {
                if (!isClosed()) {
                    Point size = getChromiumSize();
                    setCurrentSize();
                    if (OS.isWayland()) {
                        Point offset = getChromiumOffset();
                        updateUI(new Rectangle(offset.x, offset.y, size.x, size.y), null);
                    } else {
                        wasResized(size.x, size.y);
                    }
                }
            }
        });
        if (OS.isWayland()) {
            addVisibilityListeners();
        }
        if (OS.isLinux()) {
            AtomicReference<Shell> shellRef = new AtomicReference<>();
            ControlAdapter moveAdapter = new ControlAdapter() {
                @Override
                public void controlMoved(ControlEvent e) {
                    if (!isClosed()) {
                        notifyMoveOrResizeStarted();
                    }
                }
            };
            shellRef.set(composite.getShell());
            PaintListener reparentListener = new PaintListener() {
                @Override
                public void paintControl(PaintEvent e) {
                    Shell currentShell = composite.getShell();
                    Shell oldShell = shellRef.get();
                    if (currentShell != oldShell) {
                        currentShell.addControlListener(moveAdapter);
                        shellRef.set(currentShell);
                    }
                }
            };
            composite.getShell().addControlListener(moveAdapter);
            composite.addPaintListener(reparentListener);
            composite.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    Shell shell = shellRef.get();
                    if (shell != null && !shell.isDisposed()) {
                        shell.removeControlListener(moveAdapter);
                    }
                }
            });
        }
        composite.getDisplay().syncExec(() -> chromiumSize = getChromiumSize());
        createImmediately();
    }

    private void addVisibilityListeners() {
        final int visibilityDelay = 33;
        Runnable visibilitySetter = () -> {
            if (composite.isDisposed()) return;
            composite.getDisplay().syncExec(() -> { setWindowVisibility(visibleWayland); });
        };
        composite.getDisplay().addFilter(SWT.Show, (event) -> {
            boolean isAncestor = isSameOrAncestor(composite, event.widget);
            if (isAncestor && isPending_) {
                if (!visibleWayland) {
                    visibleWayland = true;
                    composite.getDisplay().timerExec(visibilityDelay, visibilitySetter);
                }
            }
        });
        composite.getDisplay().addFilter(SWT.Hide, (event) -> {
            boolean isAncestor = isSameOrAncestor(composite, event.widget);
            if (isAncestor && isPending_) {
                if (visibleWayland) {
                    visibleWayland = false;
                    composite.getDisplay().timerExec(visibilityDelay, visibilitySetter);
                }
            }
        });
        // minimize/restore part
        composite.getDisplay().addListener(SWT.Skin, (event) -> {
            if (event.widget == composite && isPending_) {
                boolean newIsVisible = composite.getShell().isVisible();
                if (!newIsVisible && visibleWayland) {
                    visibleWayland = false;
                } else if (newIsVisible && !visibleWayland) {
                    visibleWayland = true;
                }
                composite.getDisplay().timerExec(visibilityDelay, visibilitySetter);
            }
        });
    }

    private boolean isSameOrAncestor(Control control, Widget ancestor) {
        if (control.isDisposed()) return false;
        while (control != null) {
            if (control == ancestor) return true;
            control = control.getParent();
        }
        return false;
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
            if (OS.isWayland()) {
                Point offset = getChromiumOffset();
                updateUI(new Rectangle(offset.x, offset.y, size.x, size.y), null);
            } else {
                initialSize(size.x, size.y);
            }
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

    private Point getChromiumOffset() {
        org.eclipse.swt.graphics.Point location = getComposite().getDisplay().map(
                getComposite().getParent(), null, getComposite().getLocation());
        Point size = new Point(location.x, location.y);
        if (autoScaleUp != null) {
            try {
                Point scaled = new Point((int) autoScaleUp.invoke(null, size.x),
                        (int) autoScaleUp.invoke(null, size.y));
                if (scaled.x > size.x || scaled.y > size.y) size = scaled;
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
            }
        }
        return size;
    }

    private Point getChromiumSize() {
        Point size = new Point(getComposite().getSize().x, getComposite().getSize().y);
        if ("cocoa".equals(SWT.getPlatform())) {
            return size;
        }
        if (autoScaleUp != null) {
            try {
                Point scaled = new Point((int) autoScaleUp.invoke(null, size.x),
                        (int) autoScaleUp.invoke(null, size.y));
                if (scaled.x > size.x || scaled.y > size.y) size = scaled;
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
