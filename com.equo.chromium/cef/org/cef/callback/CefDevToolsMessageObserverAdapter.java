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


package org.cef.callback;

import org.cef.browser.CefBrowser;
import org.cef.misc.CefRegistration;

public class CefDevToolsMessageObserverAdapter implements CefDevToolsMessageObserver {
	CefRegistration cefRegistration;
	CefBrowser cefBrowser;

	public CefDevToolsMessageObserverAdapter(CefBrowser cefBrowser) {
		this.cefBrowser = cefBrowser;
		cefRegistration = cefBrowser.addDevToolsMessageObserver(this);
	}

	@Override
	public boolean onDevToolsMessage(CefBrowser cefBrowser, String message, int messageSize) {
		return false;
	}

	@Override
	public void onDevToolsMethodResult(CefBrowser cefBrowser, int messageId, boolean success, String result,
			int resultSize) {
	}

	@Override
	public void onDevToolsEvent(CefBrowser cefBrowser, String method, String params, int paramsSize) {

	}

	@Override
	public void onDevToolsAgentAttached(CefBrowser cefBrowser) {

	}

	@Override
	public void onDevToolsAgentDetached(CefBrowser cefBrowser) {

	}

	public void dispose() {
		cefBrowser.removeDevToolsMessageObservers(cefRegistration);
	}
}
