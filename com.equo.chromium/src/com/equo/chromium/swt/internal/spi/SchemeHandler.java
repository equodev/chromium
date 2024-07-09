package com.equo.chromium.swt.internal.spi;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

public interface SchemeHandler {

	public boolean processRequest(SchemeHandlerRequest request);

	public boolean processRequest(String url, String method, Map<String, String> headers);

	default public Charset getDefaultCharset(String mimeType) {
		return Charset.defaultCharset();
	}

	public InputStream getResponseData(Map<String, String> responseHeaders);

}
