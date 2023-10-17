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


package org.cef.handler;

import org.cef.browser.CefBrowser;
import org.cef.misc.Rectangle;

public interface CefFindHandler {
    /**
     * Called to report find results returned by CefBrowserHost::Find(). |identifer|
     * is the identifier passed to Find(), |count| is the number of matches
     * currently identified, |selectionRect| is the location of where the match was
     * found (in window coordinates), |activeMatchOrdinal| is the current position
     * in the search results, and |finalUpdate| is true if this is the last find
     * notification.
     *
     * @param browser
     * @param identifier
     * @param selectionRect
     * @param count
     * @param activeMatchOrdinal
     * @param finalUpdate
     */
    public void onFindResult(CefBrowser browser, int identifier, int count, Rectangle selectionRect,
            int activeMatchOrdinal, boolean finalUpdate);
}
