package com.equo.chromium.swt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.browser.CefBrowserSwt;

import com.equo.chromium.swt.internal.WebBrowser;

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

	/**
	 * Delete cookies matching the url and name regex patterns. Negation patterns
	 * can be used to exclude cookies from deletion.
	 * 
	 * @param urlPattern  the pattern for the matching URL case. Null matches all,
	 *                    empty matches nothing.
	 * @param namePattern the pattern for the matching cookie name case. Null
	 *                    matches all, empty matches nothing.
	 */
	static boolean clearCookies(String urlPattern, String namePattern) {
		return WebBrowser.clearCookie(urlPattern, namePattern);
	}

	/**
	 * Capture screenshot from browser
	 * @return CompletableFuture<byte[]> which will contain Base64 encoded image data.
	 */
	public CompletableFuture<byte[]> captureScreenshot();

	/**
	 * Capture screenshot of a specific area.
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param scale
	 * @return CompletableFuture<byte[]> which will contain Base64 encoded image data.
	 */
	public CompletableFuture<byte[]> captureScreenshot(int x, int y, int width, int height, int scale);

	/**
	 * Ignore certificate errors on browser. If used when the browser is defined, a
	 * new request context is created for the browser and will not affect the
	 * others. Otherwise if used with an already initialized browser, it will affect
	 * all other browser instances as the change to the global requestContent will
	 * be used.
	 * Default is false.
	 * @param enable 
	 */
	public void ignoreCertificateErrors(boolean enable);
}
