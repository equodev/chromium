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


package com.equo.chromium.swt.internal;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Base64;
import java.util.Map;

public class SetTextResourceHandler {

	private Object middlewareService;
	private static SetTextResourceHandler textResourceHandler = null;

	public static void configureScheme(String textUrl) {
		if (textResourceHandler != null)
			textResourceHandler.configureSchemes(true, textUrl);
	}


	public static void unregisterScheme(String textUrl) {
		if (textResourceHandler != null)
			textResourceHandler.configureSchemes(false, textUrl);
	}

	public void configureSchemes(boolean addResourceHandler, String textUrl) {
		try {
			URI uri = URI.create(textUrl);
			Class<?> iResponseHandlerClass = Class.forName("com.equo.middleware.api.handler.IResponseHandler");
			if (addResourceHandler) {
				Method addResourceHandlerMethod = Class.forName("com.equo.middleware.api.IMiddlewareService")
						.getDeclaredMethod("addResourceHandler",
								new Class[] { String.class, String.class, iResponseHandlerClass });
				addResourceHandlerMethod.invoke(middlewareService, new Object[] { uri.getScheme(), uri.getAuthority(),
						createResponseHandler(iResponseHandlerClass) });
			} else {
				Method removeResourceHandlerMethod = Class.forName("com.equo.middleware.api.IMiddlewareService")
						.getDeclaredMethod("removeResourceHandler", new Class[] { String.class, String.class });
				removeResourceHandlerMethod.invoke(middlewareService,
						new Object[] { uri.getScheme(), uri.getAuthority() });
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | ClassNotFoundException e) {

		}
	}

	private static Object createResponseHandler(Class<?> iResponseHandlerClass) {
		InvocationHandler inv = new InvocationHandler() {
			@SuppressWarnings("unchecked")
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if ("shouldProcessRequest".equals(method.getName())) {
					return true;
				} else if ("getResponseData".equals(method.getName())) {
					Map<String, String> requestHeaders = (Map<String, String>) Class
							.forName("com.equo.middleware.api.resource.Request").getDeclaredMethod("getHeaderMap")
							.invoke(args[0]);
					Map<String, String> responseHeaders = (Map<String, String>) args[1];
					responseHeaders.put("Content-Type", "text/html");
					byte[] dataText = Base64.getDecoder().decode(requestHeaders.getOrDefault("dataText", ""));
					return new ByteArrayInputStream(dataText);
				}
				return null;
			}
		};
		return Proxy.newProxyInstance(iResponseHandlerClass.getClassLoader(), new Class[] { iResponseHandlerClass },
				inv);
	}

	public void setMiddlewareService(Object middlewareService) {
		this.middlewareService = middlewareService;
		textResourceHandler = this;
	}

	public void unsetMiddlewareService(Object middlewareService) {
		this.middlewareService = null;
	}
}
