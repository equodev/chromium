package com.equo.chromium.swt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.browser.CefBrowserSwt;

public interface ChromiumBrowser {

	static Collection<ChromiumBrowser> getAllBrowsers() {
		Collection<ChromiumBrowser> browsers = new ArrayList<ChromiumBrowser>();
		if (CefAppState.INITIALIZED.equals(CefApp.getState())) {
			CefApp app = CefApp.getInstance();
			Set<CefClient> clients = app.getAllClients();
			for (CefClient client : clients) {
				Object[] clientBrowsers = client.getAllBrowser();
				for (Object browser : clientBrowsers) {
					if (browser instanceof CefBrowserSwt) {
						Browser composite = (Browser) ((CefBrowserSwt) browser).getComposite();
						ChromiumBrowser webBrowser = (ChromiumBrowser) composite.getWebBrowser();
						browsers.add(webBrowser);
					}
				}
			}
		}
		return browsers;
	}

	public void executeJavacript(String script);

	/**
	 * @param search Pass null to clean search
	 */
	public void find(String search, boolean forward, boolean matchCase);

	/**
	 * Change the zoom level to the specified value. Specify 0.0 to reset the zoom
	 * level.
	 * 
	 * @param zoomLevel
	 */
	public void zoom(double zoomLevel);

	public void addConsoleListener(ConsoleListener listener);

	public void removeConsoleListener(ConsoleListener listener);

	@FunctionalInterface
	public interface ConsoleListener {
		/**
		 * level: 2 (INFO), 3 (WARNING), 4 (ERROR).
		 * 
		 * @return true to stop message from being output to console.
		 */
		boolean message(int level, String message, String source, int line);
	}

	List<Object> getErrors();

	/**
	 * Early load and init the chromium engine and libraries. Usually not required.
	 */
	public static void earlyInit() throws ClassNotFoundException {
		Class.forName("com.equo.chromium.swt.internal.Engine");
	}
}
