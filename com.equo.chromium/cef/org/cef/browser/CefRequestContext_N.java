// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.callback.CefCompletionCallback;
import org.cef.callback.CefNative;
import org.cef.handler.CefRequestContextHandler;

import java.util.HashMap;
import java.util.Map;

class CefRequestContext_N extends CefRequestContext implements CefNative {
    // Used internally to store a pointer to the CEF object.
    private long N_CefHandle = 0;
    private static CefRequestContext_N globalInstance = null;
    private CefRequestContextHandler handler = null;

    @Override
    public void setNativeRef(String identifer, long nativeRef) {
        N_CefHandle = nativeRef;
    }

    @Override
    public long getNativeRef(String identifer) {
        return N_CefHandle;
    }

    CefRequestContext_N() {
        super();
    }

    static final CefRequestContext_N getGlobalContextNative() {
        CefRequestContext_N result = null;
        try {
            result = CefRequestContext_N.N_GetGlobalContext();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }

        if (globalInstance == null) {
            globalInstance = result;
        } else if (globalInstance.N_CefHandle == result.N_CefHandle) {
            result.N_CefRequestContext_DTOR();
        }
        return globalInstance;
    }

    static final CefRequestContext_N createNative(CefRequestContextHandler handler) {
        return createNative(null, handler);
    }

    static final CefRequestContext_N createNative(
            CefRequestContextSettings settings, CefRequestContextHandler handler) {
        CefRequestContext_N result = null;
        try {
            result = CefRequestContext_N.N_CreateContext(settings, handler);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        if (result != null) result.handler = handler;
        return result;
    }

    @Override
    public void dispose() {
        try {
            N_CefRequestContext_DTOR();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean isGlobal() {
        try {
            return N_IsGlobal();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean hasPreference(String name) {
        try {
            return N_HasPreference(name);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public Object getPreference(String name) {
        try {
            return N_GetPreference(name);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public Map<String, Object> getAllPreferences(boolean includeDefaults) {
        try {
            return N_GetAllPreferences(includeDefaults);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return new HashMap<String, Object>();
    }

    @Override
    public boolean canSetPreference(String name) {
        try {
            return N_CanSetPreference(name);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public String setPreference(String name, Object value) {
        try {
            return N_SetPreference(name, value);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return ule.getMessage();
        }
    }

    @Override
    public CefRequestContextHandler getHandler() {
        return handler;
    }

    @Override
    public boolean clearCertificateExceptions(CefCompletionCallback callback) {
        try {
            return N_ClearCertificateExceptions(callback);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return false;
    }

    private final static native CefRequestContext_N N_GetGlobalContext();
    private final static native CefRequestContext_N N_CreateContext(
            CefRequestContextSettings settings, CefRequestContextHandler handler);
    private final native boolean N_IsGlobal();
    private final native boolean N_HasPreference(String name);
    private final native Object N_GetPreference(String name);
    private final native Map<String, Object> N_GetAllPreferences(boolean includeDefaults);
    private final native boolean N_CanSetPreference(String name);
    private final native String N_SetPreference(String name, Object value);
    private final native void N_CefRequestContext_DTOR();
    private final native boolean N_ClearCertificateExceptions(CefCompletionCallback callback);
}
