// Copyright (c) 2024 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

/**
 * Request context initialization settings. Specify NULL or 0 to get the
 * recommended default values.
 */
public class CefRequestContextSettings {
    /**
     * The location where cache data for this request context will be stored on
     * disk. If empty then browsers will be created in "incognito mode" where
     * in-memory caches are used for storage and no data is persisted to disk.
     * HTML5 databases such as localStorage will only persist across sessions if
     * a cache path is specified. To share the global browser cache and related
     * configuration set this value to match the CefSettings.cache_path value.
     */
    public String cache_path = null;

    /**
     * To persist session cookies (cookies without an expiry date or validity
     * interval) by default when using the global cookie manager set this value
     * to true. Session cookies are generally intended to be transient and most
     * Web browsers do not persist them. Can be set globally using the
     * CefSettings.persist_session_cookies value. This value will be ignored if
     * |cache_path| is empty or if it matches the CefSettings.cache_path value.
     */
    public boolean persist_session_cookies = false;

    /**
     * Comma delimited ordered list of language codes without any whitespace
     * that will be used in the "Accept-Language" HTTP request header and
     * "navigator.language" JS attribute. Can be set globally using the
     * CefSettings.accept_language_list value or overridden on a per-browser
     * basis using the CefBrowserSettings.accept_language_list value. If all
     * values are empty then "en-US,en" will be used.
     */
    public String accept_language_list = null;

    /**
     * Comma delimited list of schemes supported by the associated
     * CefCookieManager. If |cookieable_schemes_exclude_defaults| value is
     * false the default schemes ("http", "https", "ws" and "wss") will also
     * be supported. Specifying a |cookieable_schemes_list| value and setting
     * |cookieable_schemes_exclude_defaults| value to true will disable all
     * loading and saving of cookies for this manager. Can be set globally
     * using the CefSettings.cookieable_schemes_list and
     * CefSettings.cookieable_schemes_exclude_defaults values.
     */
    public String cookieable_schemes_list = null;

    /**
     * If true the default schemes ("http", "https", "ws" and "wss") are
     * excluded from cookieable_schemes_list.
     */
    public boolean cookieable_schemes_exclude_defaults = false;

    public CefRequestContextSettings() {}

    public CefRequestContextSettings clone() {
        CefRequestContextSettings tmp = new CefRequestContextSettings();
        tmp.cache_path = cache_path;
        tmp.persist_session_cookies = persist_session_cookies;
        tmp.accept_language_list = accept_language_list;
        tmp.cookieable_schemes_list = cookieable_schemes_list;
        tmp.cookieable_schemes_exclude_defaults = cookieable_schemes_exclude_defaults;
        return tmp;
    }
}
