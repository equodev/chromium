/****************************************************************************
**
** Copyright (C) 2026 Equo
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
import org.cef.CefAppStandalone;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserStandalone;
import org.cef.browser.CefBrowserSwing;
import org.cef.browser.CefBrowserSwt;
import org.cef.browser.CefBrowserWl;
import org.cef.misc.Rectangle;

import com.equo.chromium.internal.CompatibleWithHostImpl;
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

/**
 * It provides the methods for the creation of the different types of browsers
 * SWT, Swing, Standalone, Windowless as well as the complementary methods for
 * their management.
 * 
 * All methods of this interface can be called from any thread unless otherwise
 * indicated by the specific method.
 */
public interface ChromiumBrowser {

	/**
	 * Retrieves a collection of all ChromiumBrowser instances currently active
	 * within the application.
	 * 
	 * @return Returns all active browsers.
	 * 
	 * @since 124.0.0
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
	 * Create a Windowless browser.
	 * 
	 * @param url The url that will be loaded in the browser.
	 * @return Returns a new Windowless browser instance. If an instance of
	 *         org.eclipse.swt.widgets.Display exists, returns SWT Windowless
	 *         browser or Standalone Windowless browser otherwise.
	 * 
	 * @since 124.0.0
	 */
	static ChromiumBrowser windowless(String url) {
		return new Windowless(url);
	}

	/**
	 * Create a Windowless browser with specific window bounds.
	 * 
	 * @param url    The url that will be loaded in the browser.
	 * @param x      The x coordinate of the origin of the window.
	 * @param y      The y coordinate of the origin of the window.
	 * @param width  The width of the window.
	 * @param height The height of the window.
	 * @return Returns a new Windowless browser instance. If an instance of
	 *         org.eclipse.swt.widgets.Display exists, returns SWT Windowless
	 *         browser or Standalone Windowless browser otherwise.
	 * 
	 * @since 124.0.0
	 * 
	 * @deprecated Use {@link #windowless(String, int, int, int, int, boolean)}
	 *             with {@code isolated} set to {@code false} instead.
	 */
	@Deprecated
	static ChromiumBrowser windowless(String url, int x, int y, int width, int height) {
		return new Windowless(url, new Rectangle(x, y, width, height), false);
	}

	/**
	 * Create a Windowless browser with specific window bounds, optionally
	 * isolated from other instances.
	 * <p>
	 * When {@code isolated} is {@code true}, each call creates a browser with its
	 * own in-memory request context. No cache path is assigned, so no session
	 * data is persisted to disk and all state is discarded when the instance is
	 * closed, ensuring complete session isolation between instances.
	 *
	 * @param url      The url that will be loaded in the browser.
	 * @param x        The x coordinate of the origin of the window.
	 * @param y        The y coordinate of the origin of the window.
	 * @param width    The width of the window.
	 * @param height   The height of the window.
	 * @param isolated Whether the browser should run in its own isolated,
	 *                 in-memory request context ({@code true}) or share the
	 *                 global request context ({@code false}).
	 * @return Returns a new Windowless browser instance. If an instance of
	 *         org.eclipse.swt.widgets.Display exists, returns SWT Windowless
	 *         browser or Standalone Windowless browser otherwise.
	 *
	 * @since 128.0.44
	 */
	static ChromiumBrowser windowless(String url, int x, int y, int width, int height, boolean isolated) {
		return new Windowless(url, new Rectangle(x, y, width, height), isolated);
	}

	/**
	 * Create a CompatibleWithHost constructor. This allows automatic checking of
	 * all necessary requirements for the specified toolkit, by default windowless
	 * using the specified default java system properties.
	 * 
	 * @since 116.0.20
	 */
	static CompatibleWithHost compatibleWithHost() {
		return new CompatibleWithHostImpl();
	}

	/**
	 * Create a Standalone browser.
	 * 
	 * @param url The url that will be loaded in the browser.
	 * @return Returns a new Standalone browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 * 
	 * @since 124.0.0
	 */
	static ChromiumBrowserStandalone standalone(String url) {
		IndependentBrowser.checkToolkit(BrowserType.STANDALONE);
		return new Standalone(url);
	}

	/**
	 * Create a Standalone browser with specific window bounds.
	 * 
	 * @param url    The url that will be loaded in the browser.
	 * @param x      The x coordinate of the origin of the window.
	 * @param y      The y coordinate of the origin of the window.
	 * @param width  The width of the window.
	 * @param height The height of the window.
	 * @return Returns a new Standalone browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 * 
	 * @since 124.0.0
	 */
	static ChromiumBrowserStandalone standalone(String url, int x, int y, int width, int height) {
		IndependentBrowser.checkToolkit(BrowserType.STANDALONE);
		return standalone(url, new Rectangle(x, y, width, height));
	}

	/**
	 * Create a Standalone browser with specific window bounds.
	 *
	 * @param url    The url that will be loaded in the browser.
	 * @param window Specifies the dimensions that the window will have.
	 * @return Returns a new Standalone browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 *
	 * @since 124.0.0
	 */
	static ChromiumBrowserStandalone standalone(String url, Rectangle window) {
		IndependentBrowser.checkToolkit(BrowserType.STANDALONE);
		return new Standalone(url, window);
	}

	/**
	 * Create a Standalone browser with custom title and specific window bounds.
	 * 
	 * @param url    The url that will be loaded in the browser.
	 * @param title  The window title to display.
	 * @param x      The x coordinate of the origin of the window.
	 * @param y      The y coordinate of the origin of the window.
	 * @param width  The width of the window.
	 * @param height The height of the window.
	 * @return Returns a new Standalone browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 * 
	 * @since 116.0.32
	 */
	static ChromiumBrowserStandalone standalone(String url, String title, int x, int y, int width, int height) {
		IndependentBrowser.checkToolkit(BrowserType.STANDALONE);
		return new Standalone(url, title, new Rectangle(x, y, width, height));
	}

	/**
	 * Create a Standalone browser with custom title.
	 *
	 * @param url   The url that will be loaded in the browser.
	 * @param title The window title to display.
	 * @return Returns a new Standalone browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 *
	 * @since 116.0.32
	 */
	static ChromiumBrowserStandalone standalone(String url, String title) {
		IndependentBrowser.checkToolkit(BrowserType.STANDALONE);
		return new Standalone(url, title, null);
	}

	/**
	 * Create a Standalone browser with custom title and specific window bounds.
	 *
	 * @param url    The url that will be loaded in the browser.
	 * @param title  The window title to display.
	 * @param window Specifies the dimensions that the window will have.
	 * @return Returns a new Standalone browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 *
	 * @since 116.0.32
	 */
	static ChromiumBrowserStandalone standalone(String url, String title, Rectangle window) {
		IndependentBrowser.checkToolkit(BrowserType.STANDALONE);
		return new Standalone(url, title, window);
	}

	/**
	 * Create a Swing browser.
	 * 
	 * @param container The parent container to contains the browser.
	 * @param layout    Where the window will be placed.
	 * @param url       The url that will be loaded in the browser.
	 * @return Returns a new Swing browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 * 
	 * @since 124.0.0
	 */
	static ChromiumBrowser swing(Object container, String layout, String url) {
		IndependentBrowser.checkToolkit(BrowserType.SWING);
		return new SwingBrowser(container, layout, url);
	}

	/**
	 * Create a Swing browser.
	 * 
	 * @param url The url that will be loaded in the browser.
	 * @return Returns a new Swing browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 * 
	 * @since 124.0.0
	 */
	static ChromiumBrowser swing(String url) {
		IndependentBrowser.checkToolkit(BrowserType.SWING);
		return new SwingBrowser(url);
	}

	/**
	 * Create a SWT browser.
	 * 
	 * @param composite The parent composite contains the browser.
	 * @param style     Composite style.
	 * @return Returns a new SWT browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 * 
	 * @since 124.0.0
	 */
	static ChromiumBrowser swt(Object composite, int style) {
		IndependentBrowser.checkToolkit(BrowserType.SWT);
		return (ChromiumBrowser) new Browser(composite, style).getWebBrowser();
	}

	/**
	 * Early load and initialize the Equo Chromium engine and libraries. Usually not
	 * required.
	 * 
	 * @since 124.0.0
	 */
	public static void earlyInit() throws ClassNotFoundException {
		Class.forName("com.equo.chromium.internal.Engine");
	}

	/**
	 * Start the CEF event loop when creating in Standalone and Windowless browsers.
	 * 
	 * @since 124.0.0
	 */
	public static void startBrowsers() {
		String multiThread = System.getProperty("chromium.multi_threaded_message_loop", "");
		if (!Boolean.valueOf(multiThread)) {
			Engine.startCefLoop();
		}
	}

	/**
	 * Drain pending CEF work on the calling thread. On macOS Standalone, CEF UI
	 * work must run on the same thread that initialized CEF (typically main).
	 * Callers should invoke this in a loop on that thread, e.g.:
	 *
	 * <pre>
	 *   ChromiumBrowser.standalone(url);
	 *   while (someCondition) {
	 *     ChromiumBrowser.pumpOnce();
	 *     Thread.sleep(33);
	 *   }
	 * </pre>
	 *
	 * No-op on platforms other than macOS Standalone (Windows/Linux pump CEF via
	 * multi_threaded_message_loop and {@link #startBrowsers()}).
	 *
	 * @since 128.0.44
	 */
	public static void pumpOnce() {
		CefAppStandalone.pumpMessages();
	}

	/**
	 * Forces an update of the proxy configuration.
	 * 
	 * @since 128.0.30
	 */
	void updateProxyConfig();

	/**
	 * Delete cookies matching the url and name regex patterns. Negation patterns
	 * can be used to exclude cookies from deletion.
	 * 
	 * @param urlPattern  The pattern for the matching URL case. Null matches all,
	 *                    empty matches nothing.
	 * @param namePattern The pattern for the matching cookie name case. Null
	 *                    matches all, empty matches nothing.
	 * @return Returns true if cookies were cleared and false otherwise.
	 * 
	 * @since 124.0.0
	 */
	static boolean clearCookies(String urlPattern, String namePattern) {
		return WebBrowser.clearCookie(urlPattern, namePattern);
	}

	/**
	 * Checks if the browser has been closed.
	 * 
	 * @return Returns true if the browser was closed and false otherwise.
	 * 
	 * @since 124.0.9
	 */
	boolean isClosed();

	/**
	 * Sets the URL of the browser to the specified location.
	 * 
	 * @param url The url that will be loaded in the browser.
	 * @return Returns true if the url was loaded and false otherwise.
	 * 
	 * @since 124.0.0
	 */
	boolean setUrl(String url);

	/**
	 * Sets the text content of the browser window to the specified string.
	 * 
	 * @param html The html that will be loaded in the browser.
	 * @return Returns true if the html was loaded and false otherwise.
	 * 
	 * @since 124.0.0
	 */
	boolean setText(String html);

	/**
	 * @return Returns true if the browser will be closed and false otherwise.
	 * 
	 * @since 124.0.0
	 */
	boolean close();

	/**
	 * Executes JavaScript code within the context of the browser window.
	 * 
	 * @param script Script to be executed in the browser.
	 * 
	 * @since 124.0.0
	 */
	public void executeJavaScript(String script);

	/**
	 * @deprecated Executes JavaScript code within the context of the browser
	 *             window. Use executeJavaScript(String) instead.
	 * @param script Script to be executed in the browser.
	 * 
	 * @since 124.0.0
	 */
	@Deprecated
	public void executeJavacript(String script);

	/**
	 * Finds text within the browser window based on the specified search criteria.
	 *
	 * @param search    the text to search for.
	 * @param forward   indicates whether to search forward or backward in the
	 *                  document.
	 * @param matchCase indicates whether the search should be case-sensitive.
	 * 
	 * @since 124.0.0
	 */
	public void find(String search, boolean forward, boolean matchCase);

	/**
	 * Change the zoom level to the specified value. Specify 0.0 to reset the zoom
	 * level.
	 * 
	 * @param zoomLevel The zoom level to the specified value.
	 * 
	 * @since 124.0.0
	 */
	public void zoom(double zoomLevel);

	/**
	 * Get the current zoom level.
	 * 
	 * @return Returns the current zoom level.
	 * 
	 * @since 124.0.0
	 */
	public double getZoom();

	/**
	 * Adds a listener to receive console events from the browser.
	 *
	 * @param listener The console listener to add.
	 * 
	 * @since 124.0.0
	 */
	public void addConsoleListener(ConsoleListener listener);

	/**
	 * Removes a previously added console listener from the browser.
	 *
	 * @param listener The console listener to remove.
	 * 
	 * @since 124.0.0
	 */
	public void removeConsoleListener(ConsoleListener listener);

	/**
	 * Gets all errors when the rendering process terminates unexpectedly.
	 * 
	 * @return Returns the status error list of how the process ended.
	 * 
	 * @since 124.0.0
	 */
	List<Object> getErrors();

	/**
	 * Capture screenshot from browser.
	 * 
	 * This method cannot be called from the main thread.
	 * 
	 * @return CompletableFuture<byte[]> which will contain Base64 encoded PNG image
	 *         data.
	 * 
	 * @since 124.0.0
	 */
	public CompletableFuture<byte[]> captureScreenshot();

	/**
	 * Capture screenshots of a specific area.
	 * 
	 * This method cannot be called from the main thread.
	 * 
	 * @param x      The x coordinate of the origin of the window.
	 * @param y      The y coordinate of the origin of the window.
	 * @param width  The width of the window.
	 * @param height The height of the window.
	 * @param scale  The scale of the image.
	 * @return CompletableFuture<byte[]> which will contain Base64 encoded PNG image
	 *         data.
	 * 
	 * @since 124.0.0
	 */
	public CompletableFuture<byte[]> captureScreenshot(int x, int y, int width, int height, int scale);

	/**
	 * Ignore certificate errors in the browser. If used when the browser is
	 * defined, a new request context is created for the browser and will not affect
	 * the others. Otherwise, if used with an already initialized browser, it will
	 * affect all other browser instances as the change to the global requestContent
	 * will be used. Default is false.
	 * 
	 * For browser certificate errors to be ignored it must be called upon creation,
	 * otherwise it will have an effect on all other browsers. You can reset
	 * certificate errors by calling the method with false.
	 * 
	 * @param enable
	 * 
	 * @since 124.0.0
	 */
	public void ignoreCertificateErrors(boolean enable);

	/**
	 * Listener to receive console events from the browser.
	 */
	@FunctionalInterface
	public interface ConsoleListener {
		/**
		 * level: 2 (INFO), 3 (WARNING), 4 (ERROR).
		 * 
		 * @return true to stop a message from being output to the console.
		 */
		boolean message(int level, String message, String source, int line);
	}

	/**
	 * Gets the Composite or Component UI object, depending on the current toolkit.
	 * 
	 * @return Returns the Composite or Component UI object, depending on the
	 *         current toolkit.
	 * 
	 * @since 124.0.0
	 */
	public Object getUIComponent();

	/**
	 * Checks whether the browser is currently loading a page.
	 * 
	 * @return Returns true if the browser is loading a page or false otherwise.
	 * 
	 * @since 124.0.0
	 */
	public boolean isLoading();

	/**
	 * Loads the previous location in the back-forward list.
	 * 
	 * @since 124.0.0
	 */
	public void goBack();

	/**
	 * Loads the next location in the back-forward list.
	 * 
	 * @since 124.0.0
	 */
	public void goForward();

	/**
	 * Checks whether there is a previous page to navigate back to in the browsing
	 * history.
	 * 
	 * @return Returns true if the previous location can be loaded and false
	 *         otherwise.
	 * 
	 * @since 124.0.0
	 */
	public boolean canGoBack();

	/**
	 * Checks whether there is a subsequent page to navigate forward to in the
	 * browsing history.
	 * 
	 * @return Returns true if the next location can be loaded and false otherwise.
	 * 
	 * @since 124.0.0
	 */
	public boolean canGoForward();

	/**
	 * Reloads the currently loaded web page.
	 * 
	 * @since 124.0.0
	 */
	public void reload();

	/**
	 * Cancels any pending navigation or download operation and stops any dynamic
	 * page elements, such as background sounds and animations.
	 * 
	 * @since 124.0.0
	 */
	public void stop();

	/**
	 * Gets a source text in the browser.
	 * 
	 * @return Returns a source text in the browser.
	 * 
	 * @since 124.0.0
	 */
	public CompletableFuture<String> text();

	/**
	 * Gets the current url.
	 * 
	 * @return Returns the current url.
	 * 
	 * @since 124.0.0
	 */
	public String getUrl();

	/**
	 * Returns a {@link CompletableFuture} that will be completed with the current
	 * DevTools URL. Either {@code chromium.remote_debugging_port} or
	 * {@code chromium.debug_port} must be specified.
	 *
	 * @return Returns a future that completes with the DevTools URL, or exceptionally if no
	 *         debug port is specified.
	 *
	 * @since 128.0.12
	 */
	public CompletableFuture<String> getDevtoolsUrl();

	/**
	 * Checks whether the browser has been created.
	 * 
	 * @return a CompletableFuture<Boolean> that will be completed with a boolean
	 *         value indicating whether the browser has been created
	 * 
	 * @since 124.0.0
	 */
	public CompletableFuture<Boolean> isCreated();

	/**
	 * Opens a new browser with the DevTools view of the current browser instance.
	 * 
	 * @since 124.0.0
	 */
	public void showDevTools();

	/**
	 * Close the DevTools.
	 * 
	 * @since 138.0.1
	 */
	public void closeDevTools();

	/**
	 * Gets if DevTools window is open.
	 * 
	 * @return true if DevTools window is open; false otherwise.
	 * @since 138.0.1
	 */
	public boolean hasDevTools();

	/**
	 * Print the current browser contents as a PDF.
	 * 
	 * @param path                             The path of the file to write to
	 *                                         (will be overwritten if it already
	 *                                         exists). Cannot be null.
	 * @param settings The PDF print settings to use. If
	 *                                         null then defaults will be used.
	 * 
	 * @since 124.0.0
	 */
	public CompletableFuture<Boolean> printToPdf(String path, PdfPrintSettings settings);

	/**
	 * Print the current browser contents as a PDF.
	 * 
	 * @param path The path of the file to write to (will be overwritten if it
	 *             already exists). Cannot be null.
	 * 
	 * @since 124.0.0
	 */
	public CompletableFuture<Boolean> printToPdf(String path);

	/**
	 * Returns an instance of LocalStorage.
	 * 
	 * @since 124.0.0
	 */
	public Storage getLocalStorage();

	/**
	 * Returns an instance of SessionStorage.
	 * 
	 * @since 124.0.0
	 */
	public Storage getSessionStorage();

	/**
	 * Sets the fullscreen state of the browser.
	 *
	 * @param fullscreen {@code true} to enable fullscreen, {@code false} to exit.
	 * 
	 * @since 128.0.21
	 */
	public void setFullscreen(boolean fullscreen);

	/**
	 * Returns whether the browser is currently in fullscreen mode.
	 *
	 * @return {@code true} if the browser is in fullscreen, {@code false}
	 *         otherwise.
	 * 
	 * @since 128.0.21
	 */
	public boolean isFullscreen();

	/**
	 * Toggles the fullscreen state of the browser.
	 * <p>
	 * If the browser is currently in fullscreen, this will exit fullscreen. If it
	 * is not, this will enter fullscreen.
	 * </p>
	 *
	 * @since 128.0.21
	 */
	default void toggleFullscreen() {
		setFullscreen(!isFullscreen());
	}

	/**
	 * Sets the icon of the browser window.
	 * <p>
	 * This feature is only supported for {@link Standalone} browsers on Windows.
	 * </p>
	 * <p>
	 * Only <code>.ico</code> files are supported on Windows.
	 * </p>
	 *
	 * @param icon the path to the icon file.
	 * @since 128.0.23
	 */
	public void setWindowIcon(String iconPath);

	/**
	 * Sets the title of the browser window.
	 * <p>
	 * This feature is supported for {@link Standalone} browsers on all OS.
	 *
	 * @param title the new window title to display.
	 * @since 128.0.24
	 */
	public void setWindowTitle(String title);
}
