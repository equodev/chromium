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
	 * @since 122.0.0
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
	 * @since 122.0.0
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
	 * @since 122.0.0
	 */
	static ChromiumBrowser windowless(String url, int x, int y, int width, int height) {
		return new Windowless(url, new Rectangle(x, y, width, height));
	}

	/**
	 * Create a Standalone browser.
	 * 
	 * @param url The url that will be loaded in the browser.
	 * @return Returns a new Standalone browser instance.
	 * @throws UnsupportedOperationException if another type of toolkit has been
	 *                                       initialized previously.
	 * 
	 * @since 122.0.0
	 */
	static ChromiumBrowser standalone(String url) {
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
	 * @since 122.0.0
	 */
	static ChromiumBrowser standalone(String url, int x, int y, int width, int height) {
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
	 * @since 122.0.0
	 */
	static ChromiumBrowser standalone(String url, Rectangle window) {
		IndependentBrowser.checkToolkit(BrowserType.STANDALONE);
		return new Standalone(url, window);
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
	 * @since 122.0.0
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
	 * @since 122.0.0
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
	 * @since 122.0.0
	 */
	static ChromiumBrowser swt(Object composite, int style) {
		IndependentBrowser.checkToolkit(BrowserType.SWT);
		return (ChromiumBrowser) new Browser(composite, style).getWebBrowser();
	}

	/**
	 * Early load and initialize the Equo Chromium engine and libraries. Usually not
	 * required.
	 * 
	 * @since 122.0.0
	 */
	public static void earlyInit() throws ClassNotFoundException {
		Class.forName("com.equo.chromium.internal.Engine");
	}

	/**
	 * Start the CEF event loop when creating in Standalone and Windowless browsers.
	 * 
	 * @since 122.0.0
	 */
	public static void startBrowsers() {
		String multiThread = System.getProperty("chromium.multi_threaded_message_loop", "");
		if (!Boolean.valueOf(multiThread)) {
			Engine.startCefLoop();
		}
	}

	/**
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
	 * @since 122.0.0
	 */
	static boolean clearCookies(String urlPattern, String namePattern) {
		return WebBrowser.clearCookie(urlPattern, namePattern);
	}

	/**
	 * Sets the URL of the browser to the specified location.
	 * 
	 * @param url The url that will be loaded in the browser.
	 * @return Returns true if the url was loaded and false otherwise.
	 * 
	 * @since 122.0.0
	 */
	boolean setUrl(String url);

	/**
	 * Sets the text content of the browser window to the specified string.
	 * 
	 * @param html The html that will be loaded in the browser.
	 * @return Returns true if the html was loaded and false otherwise.
	 * 
	 * @since 122.0.0
	 */
	boolean setText(String html);

	/**
	 * @return Returns true if the browser will be closed and false otherwise.
	 * 
	 * @since 122.0.0
	 */
	boolean close();

	/**
	 * Executes JavaScript code within the context of the browser window.
	 * 
	 * @param script Script to be executed in the browser.
	 * 
	 * @since 122.0.0
	 */
	public void executeJavaScript(String script);

	/**
	 * @deprecated Executes JavaScript code within the context of the browser
	 *             window. Use executeJavaScript(String) instead.
	 * @param script Script to be executed in the browser.
	 * 
	 * @since 122.0.0
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
	 * @since 122.0.0
	 */
	public void find(String search, boolean forward, boolean matchCase);

	/**
	 * Change the zoom level to the specified value. Specify 0.0 to reset the zoom
	 * level.
	 * 
	 * @param zoomLevel The zoom level to the specified value.
	 * 
	 * @since 122.0.0
	 */
	public void zoom(double zoomLevel);

	/**
	 * Get the current zoom level.
	 * 
	 * @return Returns the current zoom level.
	 * 
	 * @since 122.0.0
	 */
	public double getZoom();

	/**
	 * Adds a listener to receive console events from the browser.
	 *
	 * @param listener The console listener to add.
	 * 
	 * @since 122.0.0
	 */
	public void addConsoleListener(ConsoleListener listener);

	/**
	 * Removes a previously added console listener from the browser.
	 *
	 * @param listener The console listener to remove.
	 * 
	 * @since 122.0.0
	 */
	public void removeConsoleListener(ConsoleListener listener);

	/**
	 * Gets all errors when the rendering process terminates unexpectedly.
	 * 
	 * @return Returns the status error list of how the process ended.
	 * 
	 * @since 122.0.0
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
	 * @since 122.0.0
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
	 * @since 122.0.0
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
	 * @since 122.0.0
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
	 * @since 122.0.0
	 */
	public Object getUIComponent();

	/**
	 * Checks whether the browser is currently loading a page.
	 * 
	 * @return Returns true if the browser is loading a page or false otherwise.
	 * 
	 * @since 122.0.0
	 */
	public boolean isLoading();

	/**
	 * Loads the previous location in the back-forward list.
	 * 
	 * @since 122.0.0
	 */
	public void goBack();

	/**
	 * Loads the next location in the back-forward list.
	 * 
	 * @since 122.0.0
	 */
	public void goForward();

	/**
	 * Checks whether there is a previous page to navigate back to in the browsing
	 * history.
	 * 
	 * @return Returns true if the previous location can be loaded and false
	 *         otherwise.
	 * 
	 * @since 122.0.0
	 */
	public boolean canGoBack();

	/**
	 * Checks whether there is a subsequent page to navigate forward to in the
	 * browsing history.
	 * 
	 * @return Returns true if the next location can be loaded and false otherwise.
	 * 
	 * @since 122.0.0
	 */
	public boolean canGoForward();

	/**
	 * Reloads the currently loaded web page.
	 * 
	 * @since 122.0.0
	 */
	public void reload();

	/**
	 * Cancels any pending navigation or download operation and stops any dynamic
	 * page elements, such as background sounds and animations.
	 * 
	 * @since 122.0.0
	 */
	public void stop();

	/**
	 * Gets a source text in the browser.
	 * 
	 * @return Returns a source text in the browser.
	 * 
	 * @since 122.0.0
	 */
	public CompletableFuture<String> text();

	/**
	 * Gets the current url.
	 * 
	 * @return Returns the current url.
	 * 
	 * @since 122.0.0
	 */
	public String getUrl();

	/**
	 * Checks whether the browser has been created.
	 * 
	 * @return a CompletableFuture<Boolean> that will be completed with a boolean
	 *         value indicating whether the browser has been created
	 * 
	 * @since 122.0.0
	 */
	public CompletableFuture<Boolean> isCreated();

	/**
	 * Opens a new browser with the DevTools view of the current browser instance.
	 * 
	 * @since 122.0.0
	 */
	public void showDevTools();

	/**
	 * Print the current browser contents as a PDF.
	 * 
	 * @param path                             The path of the file to write to
	 *                                         (will be overwritten if it already
	 *                                         exists). Cannot be null.
	 * @param settings The PDF print settings to use. If
	 *                                         null then defaults will be used.
	 * 
	 * @since 122.0.0
	 */
	public CompletableFuture<Boolean> printToPdf(String path, PdfPrintSettings settings);

	/**
	 * Print the current browser contents as a PDF.
	 * 
	 * @param path The path of the file to write to (will be overwritten if it
	 *             already exists). Cannot be null.
	 * 
	 * @since 122.0.0
	 */
	public CompletableFuture<Boolean> printToPdf(String path);

	/**
	 * Returns an instance of LocalStorage.
	 * 
	 * @since 122.0.0
	 */
	public Storage getLocalStorage();

	/**
	 * Returns an instance of SessionStorage.
	 * 
	 * @since 122.0.0
	 */
	public Storage getSessionStorage();
}
