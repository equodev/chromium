// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.handler;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefNative;
import org.cef.callback.CefQueryCallback;

/**
 * Implement this interface to handle queries. All methods will be executed on the browser process
 * UI thread.
 */
public interface CefMessageRouterHandler extends CefNative {
    /**
     * Called when the browser receives a JavaScript query.
     *
     * @param browser The corresponding browser.
     * @param frame The frame generating the event. Instance only valid within the scope of this
     *         method.
     * @param queryId The unique ID for the query.
     * @param persistent True if the query is persistent.
     * @param callback Object used to continue or cancel the query asynchronously.
     * @return True to handle the query or false to propagate the query to other registered
     *         handlers, if any. If no handlers return true from this method then the query will be
     *         automatically canceled with an error code of -1 delivered to the JavaScript onFailure
     *         callback.
     */
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request,
            boolean persistent, CefQueryCallback callback);

    /**
     * Called when a pending JavaScript query is canceled.
     *
     * @param browser The corresponding browser.
     * @param frame The frame generating the event. Instance only valid within the scope of this
     *         method.
     * @param queryId The unique ID for the query.
     */
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId);
}
