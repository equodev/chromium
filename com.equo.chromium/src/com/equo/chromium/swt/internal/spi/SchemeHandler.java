package com.equo.chromium.swt.internal.spi;

import java.io.InputStream;
import java.util.Map;

public interface SchemeHandler {

	public boolean processRequest(String url, String method, Map<String, String> headers);

	public InputStream getResponseData(Map<String, String> responseHeaders);

}
