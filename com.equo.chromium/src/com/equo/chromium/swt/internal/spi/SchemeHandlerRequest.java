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
