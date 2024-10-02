package com.equo.chromium.swt.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefClientSwt;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserWl;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;
import org.cef.OS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import com.equo.chromium.ChromiumBrowser;
import com.equo.chromium.internal.Utils;

public class SWTEngine {

	
	public static void initCef(AtomicBoolean closing, AtomicBoolean shuttingDown, Runnable shutdownRunnable) {
		Display.getDefault().asyncExec(() -> {
			Display.getDefault().addListener(SWT.Close, e -> {
				closing.set(true);
			});
			Display.getDefault().disposeExec(() -> {
				if (shuttingDown.get()) {
					// already shutdown
					return;
				}
				if (!Utils.isMac() || !closing.get()) {
					shuttingDown.set(true);
					if (!Boolean.getBoolean("chromium.disable_close_windowless_before_dispose")) {
						closeWindowlessBrowsers();
					}
					shutdownRunnable.run();
				}
			});
		});
	}

	private static void closeWindowlessBrowsers() throws UnsatisfiedLinkError {
		Set<CefClient> clients = CefApp.getInstance().getAllClients();
		int allClients = clients.size();
		int wlClientsSize = 0;
		for (CefClient client : clients) {
			if (client instanceof CefClientSwt) {
				Object[] browsers = client.getAllBrowser();
				if (browsers.length > 0 && browsers[0] instanceof CefBrowserWl) {
					wlClientsSize ++;
					for (Object c : browsers) {
						((ChromiumBrowser)((CefBrowser) c).getReference()).close();
					}
				}
			}
		}
		awaitDisposeWindowless(wlClientsSize, allClients - wlClientsSize);
	}

	private static void awaitDisposeWindowless(int wlClientsSize,int expectedClients) {
		long endTime = System.currentTimeMillis() + 3000;
		while (wlClientsSize > 0 && (System.currentTimeMillis() < endTime)) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			CefApp.getInstance().doMessageLoopWork(-1);
			if (expectedClients >= CefApp.getInstance().getClientsSize()) {
				wlClientsSize = 0;
			}
		}
	}

	public static void onContextInitialized(CefApp app) {
		registerBrowserFunctions(app);
	}

	public static void registerBrowserFunctions(CefApp app) {
		app.registerSchemeHandlerFactory("https", "functions", new CefSchemeHandlerFactory() {
			@Override
			public CefResourceHandler create(CefBrowser browser, CefFrame frame,
					String schemeName, CefRequest request) {
				if (!browser.isPopup()) {
					if (isPartial(request))
						return Chromium.getChromium(browser).functionsResourceHandler.peek();
					return Chromium.getChromium(browser).createFunctionResourceHandler();
				}
				return null;
			}
			
			protected boolean isPartial(CefRequest request) {
				try {
					URI url = new URI(request.getURL());
					return url.getFragment() != null;
				} catch (URISyntaxException e) {
					return false;
				}
			}
		});
	}

	public static boolean isSystemDarkTheme() {
		String swtDark = System.getProperty("chromium.swt_dark");
		if (swtDark == null) {
			boolean isDarkTheme = false;
			try {
				Class<?> systemThemeClass = Class.forName("org.eclipse.swt.widgets.Display");
				// Method available since SWT 3.112
				Method isSystemDarkThemeMethod = systemThemeClass.getMethod("isSystemDarkTheme");
				if (isSystemDarkThemeMethod != null) {
					isDarkTheme = (boolean) isSystemDarkThemeMethod.invoke(null);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException
					| NoSuchMethodException | SecurityException e) {
			}
			return isDarkTheme;
		} else if (Boolean.parseBoolean(swtDark)) {
			return true;
		} else {
			return false;
		}
	}

	public static int getSWTVersion() {
		return SWT.getVersion();
	}

}
