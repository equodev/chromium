package com.equo.chromium.swt.internal.spi;

import java.io.IOException;
import java.io.InputStream;
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

	private final SchemeHandler schemeHandler;

	public DelegatingCefResourceHandler(SchemeHandler schemeHandler) {
		this.schemeHandler = schemeHandler;
	}

	@Override
	public boolean processRequest(CefRequest request, CefCallback callback) {
		Map<String, String> headers = new HashMap<String, String>();
		request.getHeaderMap(headers);
		boolean isRequestToProcess = schemeHandler.processRequest(request.getURL(), request.getMethod(), headers);

		if (isRequestToProcess) {
			callback.Continue();
		}
		return isRequestToProcess;
	}

	@Override
	public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
		Map<String, String> responseHeaders = new HashMap<String, String>();
		response.getHeaderMap(responseHeaders);
		responseData = schemeHandler.getResponseData(responseHeaders);
		if (responseData == null) {
			response.setStatus(404);
		} else {
			String contentType = responseHeaders.remove("Content-Type");
			if (contentType != null) {
				response.setMimeType(contentType);
			}
			response.setHeaderMap(responseHeaders);
			response.setStatus(200);
		}
	}

	@Override
	public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
		if (responseData == null) {
			return false;
		}

		try {
			int bytesReadPrim = responseData.read(dataOut, 0, bytesToRead);
			bytesRead.set(bytesReadPrim);
			if (bytesReadPrim <= bytesToRead) {
				cancel();
			}
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
