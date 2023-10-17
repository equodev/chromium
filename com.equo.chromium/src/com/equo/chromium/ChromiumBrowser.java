/****************************************************************************
**
** Copyright (C) 2022 Equo
**
** This file is part of Equo Chromium.
**
** Commercial License Usage
** Licensees holding valid commercial Equo licenses may use this file in
** accordance with the commercial license agreement provided with the
** Software or, alternatively, in accordance with the terms contained in
** a written agreement between you and Equo. For licensing terms
** and conditions see https://www.equo.dev/terms.
**
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3 as published by the Free Software
** Foundation. Please review the following
** information to ensure the GNU General Public License requirements will
** be met: https://www.gnu.org/licenses/gpl-3.0.html.
**
****************************************************************************/


package com.equo.chromium;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserStandalone;
import org.cef.browser.CefBrowserSwt;
import org.cef.browser.CefBrowserWl;
import org.cef.browser.CefBrowserSwing;
import org.cef.misc.Rectangle;

import com.equo.chromium.internal.Engine;
import com.equo.chromium.internal.Engine.BrowserType;
import com.equo.chromium.internal.IndependentBrowser;
import com.equo.chromium.internal.PopupBrowser;
import com.equo.chromium.internal.Standalone;
import com.equo.chromium.internal.SwingBrowser;
import com.equo.chromium.internal.Windowless;
import com.equo.chromium.swt.Browser;
import com.equo.chromium.swt.internal.WebBrowser;
import com.equo.chromium.utils.PdfPrintSettings;

public interface ChromiumBrowser {

	/**
     * @return Returns all active browsers.
     */
    static Collection<ChromiumBrowser> getAllBrowsers() {
        Collection<ChromiumBrowser> browsers = new ArrayList<ChromiumBrowser>();
        if (CefAppState.INITIALIZED.equals(CefApp.getState())) {
            CefApp app = CefApp.getInstance();
            Set<CefClient> clients = app.getAllClients();
            for (CefClient client : clients) {
                Object[] clientBrowsers = client.getAllBrowser();
                for (Object browser : clientBrowsers) {
                    CefBrowser castedCefBrowser = (CefBrowser) browser;
                    if (browser instanceof CefBrowserSwt) {
                        Browser composite = (Browser) ((CefBrowserSwt) browser).getComposite();
                        if (composite != null) {
                            browsers.add((ChromiumBrowser) composite.getWebBrowser());
                        } else {
                            browsers.add(new PopupBrowser((CefBrowser) browser));
                        }
                    } else if (castedCefBrowser.getReference() == null) {
                        browsers.add(new PopupBrowser((CefBrowser) browser));
                    } else if (browser instanceof CefBrowserStandalone) {
                        browsers.add((ChromiumBrowser) ((CefBrowserStandalone) browser).getReference());
                    } else if (browser instanceof CefBrowserWl) {
                        browsers.add((ChromiumBrowser) ((CefBrowserWl) browser).getReference());
                    } else if (browser instanceof CefBrowserSwing) {
                        browsers.add((ChromiumBrowser) ((CefBrowserSwing) browser).getReference());
                    }

                }
            }
        }
        return browsers;
    }

	/**
	 * @param url The url that will be loaded in the browser.
	 * @return Returns a new Windowless browser instance. If instance of
	 *         org.eclipse.swt.widgets.Display exists, returns SWT Windowless
	 *         browser or Standalone Windowless browser otherwise.
	 */
	static ChromiumBrowser windowless(String url) {
		return new Windowless(url);
	}

	/**
	 * @param url The url that will be loaded in the browser.
	 * @param x the x coordinate of the origin of the window
	 * @param y the y coordinate of the origin of the window
	 * @param width the width of the window
	 * @param height the height of the window
	 * @return Returns a new Windowless browser instance. If instance of
	 *         org.eclipse.swt.widgets.Display exists, returns SWT Windowless
	 *         browser or Standalone Windowless browser otherwise.
	 */
	static ChromiumBrowser windowless(String url, int x, int y, int width, int height) {
		return new Windowless(url, new Rectangle(x, y, width, height));
	}

	/**
	 * @param url The url that will be loaded in the browser.
	 * @return Returns a new Standalone browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 */
	static ChromiumBrowser standalone(String url) {
		IndependentBrowser.checkToolkit(BrowserType.STANDALONE);
		return new Standalone(url);
	}

	/**
	 * @param url The url that will be loaded in the browser.
	 * @param x the x coordinate of the origin of the window
	 * @param y the y coordinate of the origin of the window
	 * @param width the width of the window
	 * @param height the height of the window
	 * @return Returns a new Standalone browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 */
	static ChromiumBrowser standalone(String url, int x, int y, int width, int height) {
		IndependentBrowser.checkToolkit(BrowserType.STANDALONE);
		return standalone(url, new Rectangle(x, y, width, height));
	}

	/**
	 * @param url    The url that will be loaded in the browser.
	 * @param window Specifies the dimensions that the window will have.
	 * @return Returns a new Standalone browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 */
	static ChromiumBrowser standalone(String url, Rectangle window) {
		IndependentBrowser.checkToolkit(BrowserType.STANDALONE);
		return new Standalone(url, window);
	}

	/**
	 * @param container The parent container to contains the browser.
	 * @param layout    Where the window will be placed.
	 * @param url       The url that will be loaded in the browser.
	 * @return Returns a new Swing browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 */
	static ChromiumBrowser swing(Object container, String layout, String url) {
		IndependentBrowser.checkToolkit(BrowserType.SWING);
		return new SwingBrowser(container, layout, url);
	}

	/**
	 * @param url The url that will be loaded in the browser.
	 * @return Returns a new Swing browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 */
	static ChromiumBrowser swing(String url) {
		IndependentBrowser.checkToolkit(BrowserType.SWING);
		return new SwingBrowser(url);
	}

	/**
	 * @param composite The parent composite to contains the browser.
	 * @param style     Composite style.
	 * @return Returns a new Swt browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 */
	static ChromiumBrowser swt(Object composite, int style) {
		IndependentBrowser.checkToolkit(BrowserType.SWT);
		return (ChromiumBrowser) new Browser(composite, style).getWebBrowser();
	}

	/**
	 * Early load and init the chromium engine and libraries. Usually not required.
	 */
	public static void earlyInit() throws ClassNotFoundException {
		Class.forName("com.equo.chromium.internal.Engine");
	}

	/**
	 * Start Cef event loop when created Standalone and Windowless browsers.
	 */
	public static void startBrowsers() {
		String multiThread = System.getProperty("chromium.multi_threaded_message_loop", "");
		if (!Boolean.valueOf(multiThread)) {
			Engine.startCefLoop();
		}
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
	 * @param url The url that will be loaded in the browser.
	 * @return Returns true if the url was loaded and false otherwise.
	 */
	boolean setUrl(String url);

	/**
	 * @param html The html that will be loaded in the browser.
	 * @return Returns true if the html was loaded and false otherwise.
	 */
	boolean setText(String html);

	/**
	 * @return Returns true if the browser will be closed and false otherwise.
	 */
	boolean close();

	/**
	 * 
	 * @param script Script to be executed in the browser.
	 */
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

	/**
	 * Get current zoom level.
	 * 
	 * @return current zoom level.
	 */
	public double getZoom();

	/**
	 * Add consonleListener
	 * @param listener
	 */
	public void addConsoleListener(ConsoleListener listener);

	/**
	 * Remove consoleListener
	 * @param listener
	 */
	public void removeConsoleListener(ConsoleListener listener);

	List<Object> getErrors();

	/**
	 * Capture screenshot from browser
	 * 
	 * @return CompletableFuture<byte[]> which will contain Base64 encoded png image
	 *         data.
	 */
	public CompletableFuture<byte[]> captureScreenshot();

	/**
	 * Capture screenshot of a specific area.
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param scale
	 * @return CompletableFuture<byte[]> which will contain Base64 encoded png image
	 *         data.
	 */
	public CompletableFuture<byte[]> captureScreenshot(int x, int y, int width, int height, int scale);

	/**
	 * Ignore certificate errors on browser. If used when the browser is defined, a
	 * new request context is created for the browser and will not affect the
	 * others. Otherwise if used with an already initialized browser, it will affect
	 * all other browser instances as the change to the global requestContent will
	 * be used. Default is false.
	 * 
	 * @param enable
	 */
	public void ignoreCertificateErrors(boolean enable);

	@FunctionalInterface
	public interface ConsoleListener {
		/**
		 * level: 2 (INFO), 3 (WARNING), 4 (ERROR).
		 * 
		 * @return true to stop message from being output to console.
		 */
		boolean message(int level, String message, String source, int line);
	}

	/**
	 * @return Returns the Composite or Component UI object depending on the current
	 *         toolkit.
	 */
	public Object getUIComponent();

	/**
	 * @return Returns true if browser is loading page or false otherwise.
	 */
	public boolean isLoading();

	/**
	 * Loads the previous location in the back-forward list.
	 */
	public void goBack();

	/**
	 * Loads the next location in the back-forward list.
	 */
	public void goForward();

	/**
	 * @return Returns true if the previous location can be loaded and false
	 *         otherwise.
	 */
	public boolean canGoBack();

	/**
	 * @return Returns true if the next location can be loaded and false otherwise.
	 */
	public boolean canGoForward();

	/**
	 * Reloads the currently loaded web page.
	 */
	public void reload();

	/**
	 * Cancels any pending navigation or download operation and stops any dynamic
	 * page elements, such as background sounds and animations.
	 */
	public void stop();

	/**
	 * @return Returns a source text in browser.
	 */
	public CompletableFuture<String> text();

	/**
	 * @return Returns the current url.
	 */
	public String getUrl();

	/**
	 * 
	 * @return Returns a completableFuture that will be completed when the browser
	 *         is created.
	 */
	public CompletableFuture<Boolean> isCreated();

	/**
	 * Opens a new browser with the devtools view of the current browser instance.
	 */
	public void showDevTools();

	/**
	 * Print the current browser contents to a PDF.
	 * 
	 * @param path The path of the file to write to (will be overwritten if it already
	 *        exists). Cannot be null.
	 * @param com.equo.chromium.utils.settings The pdf print settings to use. If
	 *        null then defaults will be used.
	 */
	public CompletableFuture<Boolean> printToPdf(String path, PdfPrintSettings settings);

	/**
	 * Print the current browser contents to a PDF.
	 * 
	 * @param path The path of the file to write to (will be overwritten if it already
	 *        exists). Cannot be null.
	 */
	public CompletableFuture<Boolean> printToPdf(String path);
}
