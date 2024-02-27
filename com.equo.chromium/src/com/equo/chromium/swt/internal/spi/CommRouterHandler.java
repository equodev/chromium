package com.equo.chromium.swt.internal.spi;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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
	private static ExecutorService threadPool = Executors.newCachedThreadPool(new DaemonThreadFactory());
	private static ExecutorService queueThread = Executors.newSingleThreadExecutor(new DaemonThreadFactory());

	public CommRouterHandler(CommunicationManager commManager) {
		this.commManager = commManager;
	}

	private void handleRequest(String request, CefQueryCallback callback) {
		try {
			Optional<String> response = this.commManager.receiveMessage(request);
			if (response.isPresent()) {
				callback.success(response.get());
			}
		} catch (CommMessageError e) {
			callback.failure(e.getErrorCode(), e.getLocalizedMessage());
		}
	}

	@Override
	public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
			CefQueryCallback callback) {
		if (request.startsWith("&-")) {
			queueThread.execute(() -> {
				handleRequest(request.substring(2), callback);
			});
		} else {
			threadPool.execute(() -> {
				handleRequest(request, callback);
			});
		}
		return true;
	}

	private static class DaemonThreadFactory implements ThreadFactory {
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final ThreadGroup group;
		private final String namePrefix;

		DaemonThreadFactory() {
			java.lang.SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "comm-pool-thread-";
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}

}
