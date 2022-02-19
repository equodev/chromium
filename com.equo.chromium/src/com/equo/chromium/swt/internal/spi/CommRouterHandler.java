package com.equo.chromium.swt.internal.spi;

import java.util.Optional;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

public class CommRouterHandler extends CefMessageRouterHandlerAdapter {

	public static CefMessageRouter createRouter() {
		CefMessageRouterConfig config = new CefMessageRouterConfig("equoSend", "equoSendCancel");
		return CefMessageRouter.create(config);
	}

	private CommunicationManager commManager;

	public CommRouterHandler(CommunicationManager commManager) {
		this.commManager = commManager;
	}

	@Override
	public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
			CefQueryCallback callback) {
		Optional<String> response = this.commManager.receiveMessage(request);
		if (response.isPresent()) {
			callback.success(response.get());
		}
		return true;
	}

}
