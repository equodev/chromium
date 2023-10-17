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


package com.equo.chromium.swt.internal.spi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

public class DelegatingCefResourceHandler implements CefResourceHandler {
	private InputStream responseData;
	private Boolean processRequest;

	private final SchemeHandler schemeHandler;

	public DelegatingCefResourceHandler(SchemeHandler schemeHandler) {
		this.schemeHandler = schemeHandler;
	}

	@Override
	public boolean processRequest(CefRequest request, CefCallback callback) {
		if (processRequest == null) {
			Map<String, String> headers = new HashMap<String, String>();
			request.getHeaderMap(headers);
			boolean shouldProcessRequest = schemeHandler.processRequest(request.getURL(), request.getMethod(), headers);
			processRequest = Boolean.valueOf(shouldProcessRequest);
		}

		boolean shouldProcessRequest = processRequest.booleanValue();

		if (shouldProcessRequest && callback != null) {
			callback.Continue();
		}
		return shouldProcessRequest;
	}

	@Override
	public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
		Map<String, String> responseHeaders = new HashMap<String, String>();
		response.getHeaderMap(responseHeaders);
		responseData = schemeHandler.getResponseData(responseHeaders);
		String userStatusCodeStringify = responseHeaders.remove("X-Status-Code");
		String contentType = responseHeaders.get("Content-Type");
		int statusCode = parseStatusCode(responseData, userStatusCodeStringify);
		if (statusCode != 404) {
			if (contentType != null) {
				String mimeType = contentType;
				String charset = null;
				int index = contentType.indexOf(";");
				if (index != -1) {
					mimeType = contentType.substring(0, index);
					index = contentType.indexOf("charset=", index + 1);
					if (index != -1) {
						charset = findCharset(contentType.substring(index));
					}
				}
				response.setMimeType(mimeType);
				if (charset != null) {
					response.setCharset(charset);
				} else {
					Charset defaultCharset = schemeHandler.getDefaultCharset(mimeType);
					if (defaultCharset != null) {
						response.setCharset(defaultCharset.toString());
					}
				}
			}

			response.setHeaderMap(responseHeaders);
		}
		response.setStatus(statusCode);
	}

	private Integer parseStatusCode(InputStream responseData, String userStatusCodeStringify) {
		if (userStatusCodeStringify != null) {
			try {
				Integer userStatusCode = Integer.valueOf(userStatusCodeStringify);
				if (userStatusCode != null) {
					if (userStatusCode > 99 && userStatusCode < 600) {
						return userStatusCode;
					}
				}
			} catch (Exception e) {
				return 500;
			}
		}
		// It is mandatory check first for responseData because both can be null at the
		// same time. In that cases, its necessary check for responseData first
		if (responseData == null) {
			return 404;
		}
		if (userStatusCodeStringify == null) {
			return 200;
		}
		return 404;
	}

	private String findCharset(String contentType) {
		StringBuilder charset = new StringBuilder();
		for (int i = contentType.indexOf("=") + 1; i < contentType.length(); i++) {
			if (contentType.charAt(i) != '\"') {
				if (contentType.charAt(i) == ';') {
					return charset.toString();
				}
				charset.append(contentType.charAt(i));
			}
		}
		if (charset.length() != 0) {
			return charset.toString();
		}
		return null;
	}

	@Override
	public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
		if (responseData == null) {
			return false;
		}

		try {
			int bytesReadPrim = responseData.read(dataOut, 0, bytesToRead);
			if (bytesReadPrim == -1) {
				cancel();
				return false;
			}
			bytesRead.set(bytesReadPrim);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			cancel();
			bytesRead.set(0);
			return false;
		}
	}

	@Override
	public void cancel() {
		try {
			if (responseData != null) {
				responseData.close();
			}
		} catch (IOException e) {
		} finally {
			responseData = null;
		}
	}

}
