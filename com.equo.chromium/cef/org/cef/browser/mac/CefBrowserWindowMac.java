// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser.mac;

import org.cef.browser.CefBrowserWindow;

import java.awt.Component;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class CefBrowserWindowMac implements CefBrowserWindow {
    public long getWindowHandle(Component comp) {
        final long[] result = new long[1];
        while (comp != null) {
            if (comp.isLightweight()) {
                comp = comp.getParent();
                continue;
            }
            try {
                Class<?> accessor = Class.forName("sun.awt.AWTAccessor");
                Object componentAccessor =
                        accessor.getMethod("getComponentAccessor").invoke(accessor);
                Method getPeer = componentAccessor.getClass().getMethod("getPeer", Component.class);
                getPeer.setAccessible(true);
                Object peer = getPeer.invoke(componentAccessor, comp);
                if (isInstance(peer, "sun.lwawt.LWComponentPeer")) {
                    Object componentPeerInst =
                            Class.forName("sun.lwawt.LWComponentPeer").cast(peer);
                    Method getPlatformWindow =
                            componentPeerInst.getClass().getMethod("getPlatformWindow");
                    getPlatformWindow.setAccessible(true);
                    Object pWindow = getPlatformWindow.invoke(componentPeerInst);
                    if (isInstance(pWindow, "sun.lwawt.macosx.CPlatformWindow")) {
                        Class<?> nativeActionClass =
                                Class.forName("sun.lwawt.macosx.CFRetainedResource$CFNativeAction");
                        InvocationHandler ih = new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args)
                                    throws Throwable {
                                if ("run".equals(method.getName())) {
                                    result[0] = (long) args[0];
                                }
                                return null;
                            }
                        };
                        Object nativeActionInst =
                                Proxy.newProxyInstance(nativeActionClass.getClassLoader(),
                                        new Class[] {nativeActionClass}, ih);
                        Object platformWindowInst =
                                Class.forName("sun.lwawt.macosx.CPlatformWindow").cast(pWindow);
                        Method execute = platformWindowInst.getClass().getMethod(
                                "execute", nativeActionClass);
                        execute.setAccessible(true);
                        execute.invoke(platformWindowInst, nativeActionInst);
                        break;
                    }
                }
            } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
            }
            comp = comp.getParent();
        }
        return result[0];
    }

    private static boolean isInstance(Object instance, String clss) {
        try {
            return Class.forName(clss).isInstance(instance);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
