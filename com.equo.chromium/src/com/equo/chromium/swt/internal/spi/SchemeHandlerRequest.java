/****************************************************************************
**
** Copyright (C) 2024 Equo
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
package com.equo.chromium.swt.internal.spi;

import java.util.HashMap;
import java.util.Map;

import org.cef.browser.CefFrame;
import org.cef.network.CefRequest;

public class SchemeHandlerRequest {

	String url;
	String method;
	Map<String, String> headers;
	Frame frame;

	SchemeHandlerRequest(CefRequest request, CefFrame frame) {
		this.url = request.getURL();
		this.method = request.getMethod();
		this.headers = new HashMap<>();
		request.getHeaderMap(headers);
		this.frame = new Frame(frame.getURL(), frame.getName(), frame.isMain());
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public Frame getFrame() {
		return frame;
	}

}
