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

	public void configureSchemes() {
		if ("".equals(Chromium.SETTEXT_URL)) {
			return;
		}
		try {
			URI uri = URI.create(Chromium.SETTEXT_URL);
			Class<?> iResponseHandlerClass = Class.forName("com.equo.middleware.api.handler.IResponseHandler");
			Method addResourceHandlerMethod = Class.forName("com.equo.middleware.api.IMiddlewareService")
					.getDeclaredMethod("addResourceHandler",
							new Class[] { String.class, String.class, iResponseHandlerClass });
			addResourceHandlerMethod.invoke(middlewareService,
					new Object[] { uri.getScheme(), uri.getHost(), createResponseHandler(iResponseHandlerClass) });
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
	}

	public void unsetMiddlewareService(Object middlewareService) {
		this.middlewareService = null;
	}
}
