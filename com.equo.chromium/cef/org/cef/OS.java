// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef;

public class OS {
    private static enum OSType {
        OSUndefined,
        OSLinux,
        OSWindows,
        OSMacintosh,
        OSUnknown,
    }
    ;
    private static OSType osType = OSType.OSUndefined;

    public static final boolean isWindows() {
        return getOSType() == OSType.OSWindows;
    }

    public static final boolean isMacintosh() {
        return getOSType() == OSType.OSMacintosh;
    }

    public static final boolean isLinux() {
        return getOSType() == OSType.OSLinux;
    }

    public static final boolean isWayland() {
        String swtGtkVersion = System.getProperty("org.eclipse.swt.internal.gtk.version");
        String gdkBackend = System.getenv("GDK_BACKEND");
        String xdgSessionType = System.getenv("XDG_SESSION_TYPE");
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        if (!isLinux())
            return false;
        // GTK2 has no Wayland backend. Only honor this when SWT actually reports
        // GTK2; outside SWT the property is absent and the session env decides.
        if (swtGtkVersion != null && swtGtkVersion.startsWith("2"))
            return false;
        // Forced X11 (incl. XWayland for testing) wins over the session type.
        if ("x11".equals(gdkBackend))
            return false;
        if ("wayland".equals(xdgSessionType))
            return true;
        // WSL and similar: XDG_SESSION_TYPE unset but compositor exports WAYLAND_DISPLAY.
        return waylandDisplay != null && !waylandDisplay.isEmpty();
    }

    private static final OSType getOSType() {
        if (osType == OSType.OSUndefined) {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.startsWith("windows"))
                osType = OSType.OSWindows;
            else if (os.startsWith("linux"))
                osType = OSType.OSLinux;
            else if (os.startsWith("mac"))
                osType = OSType.OSMacintosh;
            else
                osType = OSType.OSUnknown;
        }
        return osType;
    }
}
