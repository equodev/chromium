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


package com.equo.chromium.swt.internal;

import static com.equo.chromium.internal.Engine.debug;

import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.cef.CefApp;
import org.cef.CefClientSwt;
import org.cef.CefSettings;
import org.cef.CefSettings.LogSeverity;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserSwt;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.browser.CefPopup;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefAuthCallback;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefCallback;
import org.cef.callback.CefCompletionCallback;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefCookieVisitor;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.callback.CefFileDialogCallback;
import org.cef.callback.CefJSDialogCallback;
import org.cef.callback.CefMenuModel;
import org.cef.callback.CefQueryCallback;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.CefContextMenuHandlerAdapter;
import org.cef.handler.CefDialogHandler;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefDownloadHandlerAdapter;
import org.cef.handler.CefFocusHandler.FocusSource;
import org.cef.handler.CefFocusHandlerAdapter;
import org.cef.handler.CefJSDialogHandler;
import org.cef.handler.CefJSDialogHandler.JSDialogType;
import org.cef.handler.CefJSDialogHandlerAdapter;
import org.cef.handler.CefKeyboardHandler.CefKeyEvent;
import org.cef.handler.CefKeyboardHandler.CefKeyEvent.EventType;
import org.cef.handler.CefKeyboardHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler.ErrorCode;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceHandler;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.misc.BoolRef;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefCookie;
import org.cef.network.CefCookieManager;
import org.cef.network.CefPostData;
import org.cef.network.CefPostDataElement;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.AuthenticationEvent;
import org.eclipse.swt.browser.AuthenticationListener;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.browser.VisibilityWindowListener;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.Compatibility;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import com.equo.chromium.ChromiumBrowser;
import com.equo.chromium.internal.Engine;
import com.equo.chromium.internal.IndependentBrowser;
import com.equo.chromium.swt.Browser;
import com.equo.chromium.swt.BrowserFunction;
import com.equo.chromium.swt.OpenWindowListener;
import com.equo.chromium.swt.WindowEvent;
import com.equo.chromium.swt.internal.spi.CommRouterHandler;
import com.equo.chromium.swt.internal.spi.CommunicationManager;
import com.equo.chromium.swt.internal.spi.ScriptExtension;
import com.github.cliftonlabs.json_simple.Jsoner;

@SuppressWarnings("restriction")
final class Chromium extends WebBrowser {
	private static final String DATA_TEXT_URL = "data:text/html;base64,";
	private static final int MAX_PROGRESS = 100;
	
	static {
		setupCookies();
	}
	
	private static CefClientSwt clientHandler;
	private static FocusHandler focusHandler;
	private static KeyboardHandler keyboardHandler;
	private static LifeSpanHandler lifeSpanHandler;
	private static LoadHandler loadHandler;
	private static DialogHandler dialogHandler;
	private static DisplayHandler displayHandler;
	private static RequestHandler requestHandler;
	private static JsDialogHandler jsDialogHandler;
	private static ContextMenuHandler contextMenuHandler;
	private static DownloadHandler downloadHandler;
	private static CefMessageRouter router;
	
	Browser chromium;
	OpenWindowListener[] openWindowListeners = new OpenWindowListener[0];
	private CefFocusListener focusListener;
	private String url;
	private String title = "";
	private CompletableFuture<Boolean> enableProgress = new CompletableFuture<>();
	private CompletableFuture<Boolean> created = new CompletableFuture<>();
	private CompletableFuture<Boolean> loaded = new CompletableFuture<>();
	enum Dispose {
		No, FromDispose, FromClose, FromBrowser, Unload, UnloadClosed, UnloadCancel, WaitIfClosed, DoIt, 
	}
	private Dispose disposing = Dispose.No;
	private boolean hasFocus;
	private boolean ignoreFirstFocus = true;
	private PaintListener paintListener;
	private Listener traverseListener;
	private WindowEvent isPopup;
	private ExtraApi extraApi;
	private boolean loadingPage;
	private CefBrowserSwt cefBrowser;
	Stack<FunctionsResourceHandler> functionsResourceHandler = new Stack<FunctionsResourceHandler>();
	private HashMap<String, Boolean> changedCache = new HashMap<String, Boolean>();
	private CompletableFuture<Void> urls = CompletableFuture.completedFuture(null);
	private static boolean isOpenDialog = false;
	private ControlAdapter fullscreenListener = null;
	private ResizeListener resize;
	private boolean inEvalBlocking = false;

	Chromium() {
	}
	
	public void addOpenWindowListener (OpenWindowListener listener) {
		OpenWindowListener[] newOpenWindowListeners = new OpenWindowListener[openWindowListeners.length + 1];
		System.arraycopy(openWindowListeners, 0, newOpenWindowListeners, 0, openWindowListeners.length);
		openWindowListeners = newOpenWindowListeners;
		openWindowListeners[openWindowListeners.length - 1] = listener;
	}

	public void removeOpenWindowListener (OpenWindowListener listener) {
		if (openWindowListeners.length == 0) return;
		int index = -1;
		for (int i = 0; i < openWindowListeners.length; i++) {
			if (listener == openWindowListeners[i]){
				index = i;
				break;
			}
		}
		if (index == -1) return;
		if (openWindowListeners.length == 1) {
			openWindowListeners = new OpenWindowListener[0];
			return;
		}
		OpenWindowListener[] newOpenWindowListeners = new OpenWindowListener[openWindowListeners.length - 1];
		System.arraycopy (openWindowListeners, 0, newOpenWindowListeners, 0, index);
		System.arraycopy (openWindowListeners, index + 1, newOpenWindowListeners, index, openWindowListeners.length - index - 1);
		openWindowListeners = newOpenWindowListeners;
	}
	
	public void setBrowser (Browser browser) {
		super.setBrowser(browser);
		this.chromium = browser;
	}
	
	@Override
	public void createFunction (BrowserFunction function) {
		loaded.thenRun(() -> {
			String[] arrFrames = function.getFrameNames();
			if (arrFrames == null && !function.top) {
				return;
			}

			for (BrowserFunction current : functions.values()) {
				if (current.getName().equals (function.getName())) {
					destroyFunction(current);
					break;
				}
			}
			function.index = getNextFunctionIndex();
			registerFunction(function);

			String frames = arrFrames != null && arrFrames.length > 0
					? Arrays.stream(arrFrames).collect(Collectors.joining(";", "", ";"))
					: arrFrames == null || !function.top ? ";" : "";
			int id = cefBrowser.getIdentifier();
			String encodedFn = "__browserFunction;"+function.index+";"+function.token+";"+RequestHandler.FUNCTION_HOST+";"+id+";"+function.top+";"+frames;
			CefMessageRouterConfig config = new CefMessageRouterConfig(encodedFn, function.getName());
			CefMessageRouter fnRouter = CefMessageRouter.create(config);
			getClientHandler().addMessageRouter(fnRouter);
			function.router = fnRouter;
		});
	}

	public void destroyFunction (BrowserFunction function) {
		checkBrowser();
		deregisterFunction(function);
		clientHandler.removeMessageRouter(function.router);
		function.router.dispose();
	}
	
	class FunctionsResourceHandler extends CefResourceHandlerAdapter {
		private byte[] resp = new byte[0];
		private Throwable ex;
		private int offset = 0;
		private CefCallback callback;
		private boolean eval;
		public boolean inFunction;
		@Override
		public boolean processRequest(CefRequest request, CefCallback callback) {
			if (isDisposed())
				return false;
			if (RequestHandler.isFunction(cefBrowser, request) && request.getPostData() != null) {
				try {
					int index = RequestHandler.getFunctionIndex(request);
					CefPostData data = request.getPostData();
					Vector<CefPostDataElement> elements = new Vector<CefPostDataElement>();
					data.getElements(elements);
					String encoded = "";
					for (CefPostDataElement el : elements) {
						byte[] payload = new byte[el.getBytesCount()];
						el.getBytes(payload.length, payload);
						encoded = new String(payload, StandardCharsets.UTF_8);
						debug("processRequest el: "+encoded);
						break;
					}
					Object payload = AbstractEval.decodeType(encoded, SWT.ERROR_INVALID_ARGUMENT);
					if (payload.getClass().isArray() && ((Object[])payload).length >= 2) {
						String token = (String) ((Object[])payload)[0];
						Object[] args = (Object[]) ((Object[])payload)[1];
						BrowserFunction browserFunction = functions.get(index);
						if (browserFunction != null && browserFunction.token.equals(token)) {
							if (((Object[])payload).length == 3) {
								debug("processRequest return function result: "+browserFunction.getName());
								synchronized (browserFunction) {
									if (resp.length > 0)
										callback.Continue();
									else
										this.callback = callback;
								}
							} else {
								debug("processRequest calling function: "+browserFunction.getName());
								asyncExec(() -> {
									Object ret = null;
									try {
										this.callback = callback;
										inFunction = true;
										ret = browserFunction.function(args);
										synchronized (browserFunction) {
											resp = encodeType(ret).getBytes(StandardCharsets.UTF_8);
										}
									} catch(Throwable t) {
										ex = t;
									} finally {
										inFunction = false;
										synchronized (browserFunction) {
											if (this.callback != null)
												this.callback.Continue();
											this.callback = null;
										}
									}
								});
							}
							return true;
						}
					}
				} catch (SWTException e) {
					ex = e;
					callback.Continue();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
			callback.cancel();
			return false;
		}
		
		@Override
		public void getResponseHeaders(CefResponse response, IntRef responseLength,
				StringRef redirectUrl) {
			debug("getResponseHeaders", cefBrowser);
			response.setHeaderByName("Access-Control-Allow-Origin", "*", true);
			if (ex != null) {
				response.setStatus(400);
				response.setStatusText(ex.getMessage());
				responseLength.set(0);
			} else if (eval) {
				response.setStatus(206);
				response.setMimeType("text");
				responseLength.set(resp.length);
			} else {
				response.setStatus(200);
				response.setMimeType("text");
				responseLength.set(resp.length);
			}
		}
		
		@Override
		public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead,
				CefCallback callback) {
			debug("readResponse", cefBrowser);
			if (ex != null)
				return false;
			if (offset < resp.length) {
				int transfer = Math.min(resp.length - offset, bytesToRead);
				System.arraycopy(resp, offset, dataOut, 0, transfer);
				offset += transfer;
				bytesRead.set(transfer);
				return true;
			}
			resp = new byte[0];
			if (!eval) {
				functionsResourceHandler.remove(this);
			}
			eval = false;
			offset = 0;
			ex = null;
			return false;
		}

		public void setEval(String eval) {
			if (callback != null) {
				inFunction = false;
				resp = eval.getBytes(StandardCharsets.UTF_8);
				this.eval = true;
				callback.Continue();
				callback = null;
			}
		}

	}

	@Override
	public void create(Composite parent, int style) {
		Engine.initCEF();
//		debugPrint("initCef Done");
		chromium.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		paintListener = new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				debugPrint("paintControl");
				if (chromium == null || chromium.isDisposed()) return;
				if (!Boolean.getBoolean("chromium.headless"))
					chromium.removePaintListener(this);
				createBrowser();
				paintListener = null;
			}
		};
		if (Boolean.getBoolean("chromium.headless")) {
			chromium.getDisplay().timerExec(10, () -> {if (paintListener != null) paintListener.paintControl(null);});
		} else {
			chromium.addPaintListener(paintListener);
		}
		if ("image".equals(System.getProperty("chromium.resize", "live"))) {
			resize = new ResizeListener();
		}
		if (Boolean.getBoolean("chromium.find_dialog")) {
			chromium.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (((e.stateMask & SWT.CTRL) == SWT.CTRL) && (e.keyCode == 'f') && !FindDialog.isOpen()) {
						FindDialog findDialog = new FindDialog(extraApi(), chromium.getShell());
						findDialog.open();
					}
				}
			});
		}
	}

	private void debugPrint(String log) {
		Engine.debug(log, cefBrowser);
	}
	
	private void prepareBrowser() {
		chromium.addDisposeListener(e -> {
			debugPrint("disposing chromium");
			dispose();
		});
		focusListener = new CefFocusListener();
		chromium.addFocusListener(focusListener);

		getClientHandler();
	}

	private CefClientSwt getClientHandler() {
		if (clientHandler == null || clientHandler.isDisposed()) {
			createClienthandler();
		}
		return clientHandler;
	}

	private void createBrowser() {
		prepareBrowser();
		traverseListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.type == SWT.Traverse) {
					// Set focus when navegation using TAB key.
					if (event.character == SWT.TAB && !focusListener.enabled) {
						focusListener.enabled = true;
						cefBrowser.setFocus(true);
						return;
					}

					if (!"gtk".equals(SWT.getPlatform())) {
						//debugPrint("Ignoring TRAVERSE " + event);
						event.doit = false;
					}
				}
			}
		};

		chromium.addListener(SWT.KeyDown, traverseListener); // required to take focus on tab
		if ("win32".equals(SWT.getPlatform())) {
			chromium.addListener(SWT.Traverse, traverseListener);
		}

		Net.config();
		cefBrowser = (CefBrowserSwt) clientHandler.createBrowser(this.url == null ? "about:blank" : this.url, false,
				false, extraApi().createRequestContext());
		cefBrowser.createImmediately(chromium);
	}

	@SuppressWarnings("deprecation")
	private static void createClienthandler() {
		boolean suspend = "win32".equals(SWT.getPlatform()) ? false : "true".equalsIgnoreCase(System.getProperty("chromium.suspend_threads", "true"));
		Thread main = null;
		Thread[] threads = null;
		int threadsCount = 0;
		if (suspend) {
			main = Thread.currentThread();
			threads = new Thread[Thread.activeCount()*2];
			threadsCount = Thread.enumerate(threads);
			for (int i=0; i<threadsCount; i++) {
				Thread t = threads[i];
				if (t != main) {
					t.suspend();
				}
			}
		}
		clientHandler = Engine.createClient();
		if (suspend) {
			for (int i=0; i<threadsCount; i++) {
				Thread t = threads[i];
				if (t != main) {
					t.resume();
				}
			}
		}
		setFocusHandler();
		setKeyboardHandler();
		setLifeSpanHandler();
		setLoadHandler();
		setDialogHandler();
		setDisplayHandler();
		setRequestHandler();
		setJsdialogHandler();
		setContextMenuHandler();
		setDownloadHandler();

		CommunicationManager commManager = CommunicationManager.get();
		if (commManager != null) {
			CefMessageRouter commRouter = CommRouterHandler.createRouter();
			commRouter.addHandler(new CommRouterHandler(commManager), true);
			clientHandler.addMessageRouter(commRouter);
		}

		if (!Boolean.getBoolean(ScriptExtension.DISABLE_SCRIPT_EXTENSIONS_PROPERTY)) {
			Iterator<ScriptExtension> scriptExtensions = ScriptExtension.get();
			scriptExtensions.forEachRemaining(scriptExtensionProvider -> {
				List<CefMessageRouter> routers = ScriptExtension
						.createRouter(scriptExtensionProvider.getScriptExtensions());
				routers.stream().forEach(router -> clientHandler.addMessageRouter(router));
			});
		}

		router = AbstractEval.createRouter();
		clientHandler.addMessageRouter(router);

		createClipboardRouters();
	}

	private static CefBrowser getTopParentPopup(CefBrowser browser) {
		CefBrowser parent = browser;
		while (parent != null && parent.isPopup()) {
			parent = ((CefPopup)parent).getParent();
		}
		return parent;
	}

	static Chromium getChromium(CefBrowser browser) {
		CefBrowser cefBrowser = browser;
		if (cefBrowser.isPopup()) {
			cefBrowser = getTopParentPopup(browser);
			//Popup not contains parent
			if (cefBrowser == null) return null;
		}
		Browser composite = (Browser) ((CefBrowserSwt) cefBrowser).getComposite();
		ExtraApi webBrowser = (ExtraApi) composite.getWebBrowser();
		return webBrowser.getChromium();
	}

	private static void setLifeSpanHandler() {
		lifeSpanHandler = new LifeSpanHandler();
		clientHandler.addLifeSpanHandler(lifeSpanHandler);
	}
	
	static class LifeSpanHandler extends CefLifeSpanHandlerAdapter {

		@Override
		public void onAfterCreated(CefBrowser browser) {
			debug("onAfterCreated", browser);
			if (!browser.isPopup()) {
				getChromium(browser).onAfterCreated();
			}
		}
		
		@Override
		public boolean doClose(CefBrowser browser) {
			debug("doClose", browser);
			if (!browser.isPopup()) {
				return getChromium(browser).doClose();
			}
			browser.doClose();
			return "win32".equals(SWT.getPlatform()) ? true : false;
		}

		@Override
		public void onBeforeClose(CefBrowser browser) {
			debug("onBeforeClose", browser);
			if (!browser.isPopup()) {
				getChromium(browser).onBeforeClose();
			}
		}
		
		@Override
		public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name, String popupFeatures) {
			debug("onBeforePopup", browser);
			if (!browser.isPopup()) {
				return getChromium(browser).onBeforePopup(target_url, target_frame_name, popupFeatures);
			}
			return false;
		}
	};

	private void onBeforeClose() {
		syncExec(() -> fireCloseListener());
		for (BrowserFunction function : new ArrayList<>(functions.values())) {
			function.dispose(true);
		}
		functions.clear();
		if (disposing == Dispose.FromBrowser) {
			syncExec(() -> chromium.dispose());
		}

		this.chromium = null;
		debugPrint("closed");
		this.cefBrowser = null;
	}

	private void fireCloseListener() {
		if (chromium != null) {
			Display display = Display.getDefault();
			org.eclipse.swt.browser.WindowEvent event = new org.eclipse.swt.browser.WindowEvent(chromium);
			event.display = display;
			event.widget = chromium;
			for (CloseWindowListener listener : closeWindowListeners) {
				listener.close(event);
			}
		}
	}

	private boolean doClose() {
//		if (!ChromiumLib.cefswt_is_same(Chromium.this.browser, browser)) {
//			debugPrint("DoClose popup:" + Chromium.this.browser+":"+browser);
//			return false;
//		}

		if (disposing == Dispose.FromClose || disposing == Dispose.Unload || disposing == Dispose.UnloadClosed || disposing == Dispose.WaitIfClosed) {
			disposing = Dispose.DoIt;
		}
		else if (disposing == Dispose.No) {
			if (chromium != null) {
				disposing = Dispose.FromBrowser;
				asyncExec(chromium::dispose);
			}
		}
		debugPrint("AFTER DoClose");
		return true;
	}
	
	private void onAfterCreated() {
		if (isDisposed()) return;
		
//			if (this.isPopup != null && this.url != null) {
//				debugPrint("load url after created");
//				doSetUrlPost(browser, url, postData, headers);
//			}
//			else if (!"about:blank".equals(this.url)) {
//				enableProgress.complete(true);
//			}
		syncExec(() -> cefBrowser.resize());
		created.complete(true);
		loaded.complete(true);
		if (this.url != null) {
			enableProgress.complete(true);
		}

		if (isPopup != null) {
			syncExec(() -> {
				org.eclipse.swt.browser.WindowEvent event = new org.eclipse.swt.browser.WindowEvent(chromium);
				event.display = chromium.getDisplay ();
				event.widget = chromium;
				event.size = isPopup.size;
				event.location = isPopup.location;
				event.addressBar = isPopup.addressBar;
				event.menuBar = isPopup.menuBar;
				event.statusBar = isPopup.statusBar;
				event.toolBar = isPopup.toolBar;
				
				if (event.size != null && !event.size.equals(new Point(0,0))) {
					Point size = event.size;
					chromium.getShell().setSize(chromium.getShell().computeSize(size.x, size.y));
				}
				
				for (VisibilityWindowListener listener : visibilityWindowListeners) {
					listener.show(event);
				}
			});
		}
	}

	private boolean onBeforePopup(String target_url, String target_frame_name, String popupFeatures) {
		if (isDisposed())
			return true;
		WindowEvent event = new WindowEvent(chromium);
		event.data = target_url;
		event.display = chromium.getDisplay ();
		event.widget = chromium;
		event.required = false;
		event.addressBar = popupFeatures.charAt(0) == '1';
		event.menuBar = popupFeatures.charAt(1) == '1';
		event.statusBar = popupFeatures.charAt(2) == '1';
		event.toolBar = popupFeatures.charAt(0) == '1';
//			int x = popupFeatures.xSet == 1 ? popupFeatures.x : 0 ;
//			int y = popupFeatures.ySet == 1 ? popupFeatures.y : 0 ;
//			event.location = popupFeatures.xSet == 1 || popupFeatures.ySet == 1 ? new Point(x, y) : null;
//			int width = popupFeatures.widthSet == 1 ? popupFeatures.width : 0;
//			int height = popupFeatures.heightSet == 1 ? popupFeatures.height : 0;
//			event.size = popupFeatures.widthSet == 1 || popupFeatures.heightSet == 1 ? new Point(width, height) : null;

		syncExec(() -> {
			for (OpenWindowListener listener : openWindowListeners) {
				listener.open(event);
			}
		});

		if (event.browser != null) {
			//TODO handle popup when event.browser is received
//			if (((Chromium) event.browser.webBrowser).cefBrowser == null) {
//				((Chromium) event.browser.webBrowser).createPopup(windowInfo, client, event);
//			} else if (event.browser == this.chromium) {
				if (target_url != null) {
					setUrl(target_url, null, null);
				}
				return true;
//			}
		}
		else if (event.browser == null && event.required)
			return true;
		return false;
	}

	private static void setLoadHandler() {
		loadHandler = new LoadHandler();
		clientHandler.addLoadHandler(loadHandler);
	}
	
	static class LoadHandler extends CefLoadHandlerAdapter {
		@Override
		public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack,
				boolean canGoForward) {
//			debug("onLoadingStateChange", browser);
			if (!browser.isPopup()) {
				getChromium(browser).on_loading_state_change(isLoading, canGoBack, canGoForward);
			}
		}
	}
	
	private void on_loading_state_change(boolean isLoading, boolean canGoBack, boolean canGoForward) {
		debug("on_loading_state_change: "+isLoading);
		if (isDisposed()) return;
		if (isPopup != null) {
			enableProgress.complete(true);
		}
		else if (!enableProgress.isDone() && !isLoading) {
			enableProgress.complete(true);
			return;
		}
		else if (!enableProgress.isDone()) {
			return;
		}

		if (!isLoading) {
			ProgressEvent event = new ProgressEvent(chromium);
			event.display = chromium.getDisplay ();
			event.widget = chromium;
			event.current = MAX_PROGRESS;
			event.total = MAX_PROGRESS;

			if (loadingPage) {
				loadingPage = false;
				debugPrint("progress completed"); 
				asyncExec(() -> {
					if (isDisposed()) return;
					for (ProgressListener listener : progressListeners) {
						listener.completed(event);
					}
				});
			}
		}
	}

	private static void setDialogHandler() {
		dialogHandler = new DialogHandler();
		clientHandler.addDialogHandler(dialogHandler);
	}

	static class DialogHandler implements CefDialogHandler {
		@Override
		public boolean onFileDialog(CefBrowser browser, FileDialogMode mode, String title, String defaultFilePath,
				Vector<String> acceptFilters, CefFileDialogCallback callback) {
			// debug("onFileDialog", browser);
			Chromium ch = getChromium(browser);
			if (ch != null) {
				return ch.onFileDialog(mode, title, defaultFilePath, acceptFilters,
						callback);
			}
			return false;
		}
	}

	private boolean onFileDialog(CefDialogHandler.FileDialogMode mode, String title, String defaultFilePath,
			Vector<String> acceptFilters, CefFileDialogCallback callback) {
		if ("gtk".equals(SWT.getPlatform())) {
			// Prevent open multiple dialog. Block ui
			if (isOpenDialog) {
				return true;
			}

			int swtMode;
			String defaultDlgTitle;
			switch (mode) {
			case FILE_DIALOG_OPEN_MULTIPLE:
				swtMode = SWT.OPEN | SWT.MULTI;
				defaultDlgTitle = "Open Files";
				break;
			case FILE_DIALOG_SAVE:
				swtMode = SWT.SAVE;
				defaultDlgTitle = "Save File";
				break;
			default:
				swtMode = SWT.OPEN;
				defaultDlgTitle = "Open File";
				break;
			}

			String dlgTitle = title == null || title.isEmpty() ? defaultDlgTitle : title;

			Thread thread = preventBlockUI();
			asyncExec(() -> {
				FileDialog dlg = new FileDialog(chromium.getShell(), swtMode | SWT.TOP);
				dlg.setText(dlgTitle);
				String[] acceptFiltersArray = MimeTypeLinux.getExtensions(acceptFilters);
				dlg.setFilterExtensions(acceptFiltersArray);
				dlg.setOverwrite(true);

				isOpenDialog = true;
				thread.start();
				String selected = dlg.open();
				isOpenDialog = false;
				if (selected != null) {
					Vector<String> filePaths = new Vector<String>();
					for (String fileName : dlg.getFileNames()) {
						filePaths.add(Paths.get(dlg.getFilterPath(), fileName).toString());
					}
					callback.Continue(filePaths);
				} else {
					callback.Cancel();
				}
			});
			return true;
		}
		return false;
	}

	private static void setDisplayHandler() {
		displayHandler = new DisplayHandler();
		clientHandler.addDisplayHandler(displayHandler);
	}

	static class DisplayHandler extends CefDisplayHandlerAdapter {
		@Override
		public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
			debug("onAddressChange", browser);
			if (!browser.isPopup()) {
				getChromium(browser).onAddressChange(frame, url);
			}
		}

		@Override
		public void onTitleChange(CefBrowser browser, String title) {
//			debug("onTitleChange", browser);
			if (!browser.isPopup()) {
				getChromium(browser).onTitleChange(title);
			}
		}

		@Override
		public void onStatusMessage(CefBrowser browser, String value) {
//			debug("onStatusMessage", browser);
			if (!browser.isPopup()) {
				getChromium(browser).onStatusMessage(value);
			}
		}

		@Override
		public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message,
				String source, int line) {
			if (!browser.isPopup()) {
				return getChromium(browser).onConsoleMessage(level, message, source, line);
			}
			return false;
		}

		@Override
		public void onLoadingProgressChange(CefBrowser browser, double progress) {
			if (!browser.isPopup()) {
				getChromium(browser).onLoadingProgressChange(browser, progress);
			}
		}

		@Override
		public void onFullscreenModeChange(CefBrowser browser, boolean fullscreen) {
			if (!browser.isPopup()) {
				getChromium(browser).onFullscreenModeChange(browser, fullscreen);
			}
		}
	}
	
	private void onTitleChange(String title) {
		if (isDisposed()) return;
		String str = getPlainUrl(title);
		TitleEvent event = new TitleEvent(chromium);
		event.display = chromium.getDisplay();
		event.widget = chromium;
		event.title = str;
		this.title = str != null ? str : "";
		asyncExec(() -> {
			for (TitleListener listener : titleListeners) {
				listener.changed(event);
			}
		});
	}

	private void onAddressChange(CefFrame frame, String url) {
		if (isDisposed()) return;

		if (allowTurboLink() && !"about:blank".equals(url)) {
			// Filter changed event when there was no changing event.
			if (!changedCache.containsKey(url)) {
				return;
			}
			changedCache.remove(url);
		}
		
		LocationEvent event = new LocationEvent(chromium);
		event.display = chromium.getDisplay();
		event.widget = chromium;
		event.doit = true;
		event.location = getPlainUrl(url);
		event.top = frame.isMain();
		if (!enableProgress.isDone()) {
			debugPrint("!on_address_change to " + event.location + " " + (event.top ? "main" : "!main"));
			return;
		}
		debugPrint("on_address_change to " + event.location + " " + (event.top ? "main" : "!main"));
		boolean crossOrigin = true;
		if (crossOrigin) {
			for (BrowserFunction current : functions.values()) {
				getClientHandler().removeMessageRouter(current.router);
				getClientHandler().addMessageRouter(current.router);
			}
		}
		asyncExec(() -> {
			for (LocationListener listener : locationListeners) {
				listener.changed(event);
			}
			loaded.complete(true);
		});
	}

	private void onStatusMessage(String status) {
		if (isDisposed()) return;
		StatusTextEvent event = new StatusTextEvent(chromium);
		event.display = chromium.getDisplay ();
		event.widget = chromium;
		event.text = status;
		asyncExec(() -> {
			for (StatusTextListener listener : statusTextListeners) {
				listener.changed(event);
			}
		});
	}

	private boolean onConsoleMessage(CefSettings.LogSeverity level, String message, String source, int line) {
		if (isDisposed()) return false;
		AtomicBoolean prevent = new AtomicBoolean();
		if (extraApi != null) {
			syncExec(() -> {
				prevent.set(extraApi.onConsoleMessage(cefBrowser, level, message, source, line));
			});
		}
		return prevent.get();
	}

	private void onLoadingProgressChange(CefBrowser browser, double progress) {
		if (!enableProgress.isDone() || isDisposed()) {
			return;
		}

		ProgressEvent event = new ProgressEvent(chromium);
		event.display = chromium.getDisplay ();
		event.widget = chromium;
		event.current = (int)(progress * MAX_PROGRESS);
		event.total = MAX_PROGRESS;
		if (MAX_PROGRESS != event.current) {
			asyncExec(() -> {
				for (ProgressListener listener : progressListeners) {
					listener.changed(event);
				}
			});
		}
	}

	private void onFullscreenModeChange(CefBrowser browser, boolean fullScreen) {
		if (isDisposed()) return;
		
		syncExec(() -> {
			chromium.getShell().setFullScreen(fullScreen);
			Shell cefBrowserShell = cefBrowser.getComposite().getShell();
			
			if (!fullScreen) {
				if (fullscreenListener != null) {
					cefBrowserShell.removeControlListener(fullscreenListener);
					fullscreenListener = null;
				}
				cefBrowser.resize();
				return;
			}
			
			// Listen fullscreen changes from swt shell
			fullscreenListener = new ControlAdapter () {
				boolean isFirtsResized = !"win32".equals(SWT.getPlatform());
				
				@Override
				public void controlResized(ControlEvent arg0) {
					if (!isFirtsResized) {
						execute("if(document.exitFullscreen) { document.exitFullscreen(); }");
					}
					isFirtsResized = false;
				}
			};
			
			cefBrowserShell.addControlListener(fullscreenListener);
		});
	}

	private static void setRequestHandler() {
		requestHandler = new RequestHandler();
		clientHandler.addRequestHandler(requestHandler);
	}

	private static boolean allowTurboLink() {
		String[] turbolink = System.getProperty("chromium.turbolinks", "").split(";");
		return "true".equals(turbolink[0]) || turbolink[0].contains("=");
	}

	static class RequestHandler extends CefRequestHandlerAdapter {
		private static final String FUNCTION_HOST = "functions";

		@Override
		public void onRenderProcessTerminated(CefBrowser browser, TerminationStatus status) {
			if (!browser.isPopup()) {
				getChromium(browser).extraApi().error(status);
			}
		}
		
		@Override
		public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture,
				boolean is_redirect) {
//			debug("onBeforeBrowse", browser);
			if (!browser.isPopup()) {
				return getChromium(browser).onBeforeBrowse(frame, request);
			}
			return false;
		}
		
		@Override
		public boolean getAuthCredentials(CefBrowser browser, String origin_url, boolean isProxy, String host, int port,
				String realm, String scheme, CefAuthCallback callback) {
			Chromium chm = getChromium(browser);
			if (chm != null) {
				chm.getAuthCredentials(origin_url, host, port, realm, scheme, callback);
				return true;
			}
			return false;
		}
		
		@Override
		public boolean onCertificateError(CefBrowser browser, ErrorCode cert_error, String request_url,
				CefCallback callback) {
			debug("onCertificateError", browser);
			Chromium chm = getChromium(browser);
			if (chm != null) {
				return chm.onCertificateError(cert_error, request_url, callback);
			}
			return false;
		}
		
		@Override
		public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame,
				CefRequest request, boolean isNavigation, boolean isDownload, String requestInitiator,
				BoolRef disableDefaultHandling) {
			boolean isCustomProtocol = false;
			if (isFunction(browser, request)) {
				return null;
			} else if (Engine.isRegisteredProtocol(request.getURL())) {
				return null;
			} else {
				String requestUrl = request.getURL();
				if (!requestUrl.startsWith("http") && !requestUrl.startsWith("data") && !requestUrl.startsWith("file")
						&& !requestUrl.startsWith("chrome") && !requestUrl.startsWith("about")) {
					if (shouldAllowProtocol(requestUrl, browser)) {
						return new CefResourceRequestHandlerAdapter() {
							@Override
							public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
								return false;
							}
							@Override
							public void onProtocolExecution(CefBrowser browser, CefFrame frame, CefRequest request,
									BoolRef allowOsExecution) {
								debug("onProtocolExecution: " + request.getURL(), browser);
								browser.stopLoad();
								Program.launch(requestUrl);
							}
						};
					} else {
						isCustomProtocol = true;
					}
				}
			}

			if (((allowTurboLink() && containsTurbolinkHeader(request))) || isCustomProtocol) {
				return new CefResourceRequestHandlerAdapter() {
					@Override
					public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
						return true;
					}
				};
			}
			return null;
		}

		private boolean containsTurbolinkHeader(CefRequest request) {
			Map<String, String> headers = new HashMap<String, String>();
			request.getHeaderMap(headers);
			String[] turbolinkHeaders = System.getProperty("chromium.turbolinks", "").split(";");
			for (String turbolinkHeader : turbolinkHeaders) {
				if (turbolinkHeader.contains("=")) {
					String hKey = turbolinkHeader.split("=")[0];
					String hValue = turbolinkHeader.split("=")[1];
					if (headers.containsKey(hKey) && headers.get(hKey).equals(hValue)) {
						return true;
					}
				}
			}
			return "true".equals(request.getHeaderByName("x-pjax"))
					|| !request.getHeaderByName("Turbo-Frame").isEmpty()
					|| ("empty".equals(request.getHeaderByName("sec-fetch-dest"))
							&& !"navigate".equals(request.getHeaderByName("Sec-Fetch-Mode"))
							&& !"websocket".equals(request.getHeaderByName("Sec-Fetch-Mode")));
		}

		static private boolean isFunction(CefBrowser browser, CefRequest request) {
			if (!"POST".equals(request.getMethod()) || !request.getURL().contains(FUNCTION_HOST))
				return false;
			try {
				int functionIndex = getFunctionIndex(request);

				if (!browser.isPopup()) {
					return getChromium(browser).functions.containsKey(functionIndex);
				}
				return false;
			} catch (NumberFormatException | URISyntaxException e) {
				return false;
			}
		}

		protected boolean isPartial(CefRequest request) {
			try {
				URI url = new URI(request.getURL());
				return url.getFragment() != null;
			} catch (URISyntaxException e) {
				return false;
			}
		}

		private static int getFunctionIndex(CefRequest request) throws URISyntaxException {
			URI url = new URI(request.getURL());
			return Integer.parseInt(url.getQuery());
		}
	
		private boolean shouldAllowProtocol(String requestUrl, CefBrowser browser) {
			String[] protocolProps = System.getProperty("chromium.custom_protocol", "").split(";");
			if (protocolProps.length > 1) {
				for (String protocolAllow : protocolProps) {
					if (requestUrl.startsWith(protocolAllow)) {
						return true;
					}
				}
				return false;
			}

			String allowType = protocolProps[0].toLowerCase();
			if ("true".equals(allowType)) {
				return true;
			}
			if ("confirm".equals(allowType)) {
				CompletableFuture<Boolean> dialogSuccess = new CompletableFuture<>();
				CefJSDialogCallback callback = new CefJSDialogCallback() {

					@Override
					public void Continue(boolean success, String user_input) {
						dialogSuccess.complete(success);
					}
				};
				Chromium chm = getChromium(browser);
				if (chm != null) chm.openJsDialog(browser.isPopup(), CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_CONFIRM, "",
						"The link will be opened with the operating system.", "Do you want to continue?",
						"default_prompt_text", callback);

				try {
					return dialogSuccess.get();
				} catch (Exception e) {
				}
			}

			return false;
		}
	}

	private boolean onBeforeBrowse(CefFrame frame, CefRequest request) {
		if (isDisposed()) return false;
		inEvalBlocking=true;
		LocationEvent event = new LocationEvent(chromium);
		event.display = chromium.getDisplay();
		event.widget = chromium;
		event.doit = true;
		event.top = frame.isMain();
		event.location = getPlainUrl(request.getURL());
		debugPrint("on_before_browse:" + event.location + " top:"+event.top);
		try {
//			loopDisable = true;
			syncExec(() -> {
				for (LocationListener listener : locationListeners) {
					listener.changing(event);
				}
			});
		} finally {
//			loopDisable = false;
			inEvalBlocking=false;
		}

		if(allowTurboLink()) {
			changedCache.put(event.location, true);
		}

		if (!event.doit) {
			debugPrint("canceled nav");
			enableProgress = new CompletableFuture<>();
		} else {
			loadingPage = true;
		}

		return event.doit ? false : true;
	}

	protected CefResourceHandler createFunctionResourceHandler() {
		functionsResourceHandler.push(new FunctionsResourceHandler());
		return functionsResourceHandler.peek();
	}

	private void getAuthCredentials(String origin_url, String host, int port, String realm, String scheme, CefAuthCallback callback) {
		if (isDisposed()) return;
		String protocol = "http";
		try {
			URL u = new URL(this.url);
			protocol = u.getProtocol();
		} catch (MalformedURLException e) {
		}
		AuthenticationEvent event = new AuthenticationEvent(chromium);
		event.display = chromium.getDisplay();
		event.widget = chromium;
		event.doit = true;
		event.location = protocol + "://" + host;
		debugPrint("get_auth_credentials: " + event.location);
		syncExec(() -> {
			for (AuthenticationListener listener : authenticationListeners) {
				listener.authenticate(event);
			}
			if (event.doit == true && event.user == null && event.password == null) {
				new AuthDialog(chromium.getShell()).open(event, realm, callback);
			}
			if (event.doit) {
				callback.Continue(event.user, event.password);
			} else {
				callback.cancel();
			}
		});
	}

	class AuthDialog extends Dialog {

		public AuthDialog(Shell parent) {
			super(parent);
		}
		
		public void open(AuthenticationEvent authEvent, String realm, CefAuthCallback callback) {
			Shell parent = getParent();
			Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
			shell.setText("Authentication Required");
			GridLayout layout = new GridLayout(2, false);
			layout.marginHeight = 10;
			layout.marginWidth = 10;
			shell.setLayout(layout);
			Label info = new Label(shell, SWT.WRAP);
			StringBuilder infoText = new StringBuilder(authEvent.location);
			infoText.append(" is requesting you username and password.");
			if (realm != null) {
				infoText.append(" The site says: \"").append(realm).append("\"");
			}
			info.setText(infoText.toString());
			info.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

			Label label1 = new Label(shell, SWT.NONE);
			label1.setText("User Name: ");
			Text username = new Text(shell, SWT.SINGLE | SWT.BORDER);
			username.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			Label label2 = new Label(shell, SWT.NONE);
			label2.setText("Password: ");
			Text password = new Text(shell, SWT.SINGLE | SWT.BORDER);
			password.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			password.setEchoChar('*');
			
			Composite bar = new Composite(shell, SWT.NONE);
			bar.setLayoutData(new GridData(SWT.END, SWT.END, false, true, 2, 1));
			bar.setLayout(new GridLayout(2, true));
			Button cancelButton = new Button(bar, SWT.PUSH);
			cancelButton.setText("Cancel");
			cancelButton.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					authEvent.doit = false;
					shell.close();
					callback.cancel();
				}
			});
			GridData cancelData = new GridData(SWT.CENTER, SWT.END, false, false);
			cancelData.widthHint = 80;
			cancelButton.setLayoutData(cancelData);

			Button okButton = new Button(bar, SWT.PUSH);
			okButton.setText("Ok");
			okButton.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					authEvent.user = username.getText();
					authEvent.password = password.getText();
					shell.close();
					callback.Continue(authEvent.user, authEvent.password);
				}
			});
			GridData okData = new GridData(SWT.CENTER, SWT.END, false, false);
			okData.minimumWidth = SWT.DEFAULT;
			okData.widthHint = 80;
			okButton.setLayoutData(okData);
			shell.pack();
			shell.open();
			Display display = parent.getDisplay();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		}
	}

	private boolean onCertificateError(ErrorCode cert_error, String request_url, CefCallback callback) {
		if (extraApi().isIgnoreCertificateErrors()) {
			callback.Continue();
			return true;
		}
		if (extraApi().handleCertificateProperty()) {
			return false;
		} else {
			asyncExec(() -> {
				String javaHost = request_url;
				Consumer<Integer> close = result -> {
					if (result == SWT.YES) {
						callback.Continue();
					} else {
						callback.cancel();
					}
				};
				if ("test".equals(System.getProperty("chromium.dialogs", ""))) {
					headlessDialog(close);
					return;
				}
				
				MessageBox prompt = new MessageBox (chromium.getShell(), SWT.YES | SWT.NO); 
				prompt.setText(SWT.getMessage("SWT_InvalidCert_Title"));
				String cefError = cert_error.toString();
				String specific = cefError.isEmpty() ? "\n\n" : "\n\n" + cefError + "\n\n";
				String message = SWT.getMessage("SWT_InvalidCert_Message", new Object[] {javaHost}) +
						specific + SWT.getMessage("SWT_InvalidCert_Connect");
				prompt.setMessage(message);
				int result = prompt.open();
				close.accept(result);
			});
			return true;
		}
	}

	private static void setJsdialogHandler() {
		jsDialogHandler = new JsDialogHandler();
		clientHandler.addJSDialogHandler(jsDialogHandler);
	}
	
	static class JsDialogHandler extends CefJSDialogHandlerAdapter {
		@Override
		public boolean onJSDialog(CefBrowser browser, String origin_url, JSDialogType dialog_type, String message_text,
				String default_prompt_text, CefJSDialogCallback callback, BoolRef suppress_message) {
			debug("onJSDialog", browser);
			Chromium chm = getChromium(browser);
			if (!useSwtDialogs() || chm == null) {
				return false;
			}
			return chm.onJsdialog(browser, origin_url, dialog_type, message_text, default_prompt_text, callback, suppress_message);

		}

		@Override
		public boolean onBeforeUnloadDialog(CefBrowser browser, String message_text, boolean is_reload,
				CefJSDialogCallback callback) {
			debug("onBeforeUnloadDialog", browser);
			if (!browser.isPopup()) {
				return getChromium(browser).onBeforeUnloadDialog(message_text, is_reload, callback);
			}
			return false;
		}

		@Override
		public void onDialogClosed(CefBrowser browser) {
			debug("onDialogClosed", browser);
			if (!browser.isPopup()) {
				getChromium(browser).onDialogClosed();
			}
		}
	}
	
	private boolean onJsdialog(CefBrowser browser, String origin_url, JSDialogType dialog_type, String message_text,
			String default_prompt_text, CefJSDialogCallback callback, BoolRef suppress_message) {
		if (isDisposed() && !browser.isPopup()) return false;
		
		String prompt = default_prompt_text;
		String url = origin_url;
		String title = getPlainUrl(url);
		String use = System.getProperty("chromium.dialogs", "swt");
		switch (use) {
		case "swt-host":
			try {
				title = new URL(getPlainUrl(url)).getHost();
			} catch (MalformedURLException e) {
				title = "";
			}
			break;
		case "swt-empty":
			title = "";
			break;
		case "swt-title":
			title = this.title;
			break;
		}
		openJsDialog(browser.isPopup(), dialog_type, title, message_text, prompt, default_prompt_text, callback);
		return true;
	}

	private Shell getShell(boolean condition, AtomicBoolean disposeDialog) {
		if (condition) {
			return chromium.getShell();
		} else {
			disposeDialog.set(true);
			return new Shell();
		}
	}

	public void openJsDialog(boolean isPopup, JSDialogType dialog_type, String title, String msg, String prompt,
			String default_prompt_text, CefJSDialogCallback callback) {
		Consumer<Integer> close = open -> {
			boolean r = open == SWT.OK || open == SWT.YES ? true : false;
			debug("JS Dialog Closed with "+r);
			if(browser != null && !isPopup) {
				if (disposing == Dispose.Unload && !r)
					disposing = Dispose.UnloadCancel;
				asyncExec(() -> {
					callback.Continue(r, default_prompt_text);
				});
				chromium.getShell().forceActive();
			} else {
				asyncExec(() -> {
					callback.Continue(r, default_prompt_text);
				});
			}
		};
		asyncExec(() -> {
			int style = SWT.ICON_WORKING;
			switch (dialog_type) {
			case JSDIALOGTYPE_ALERT:
				style = SWT.ICON_INFORMATION;
				break;
			case JSDIALOGTYPE_CONFIRM:
				style = SWT.ICON_WARNING | SWT.CANCEL | SWT.OK;
				break;
			case JSDIALOGTYPE_PROMPT:
				style = SWT.ICON_QUESTION | SWT.CANCEL | SWT.OK;
				break;
			}
			if (!"test".equals(System.getProperty("chromium.dialogs", "")) || browser == null) {
				AtomicBoolean disposeDialog = new AtomicBoolean(false);
				Shell shell = getShell((browser != null && !isPopup), disposeDialog);
				MessageBox box = new MessageBox(shell, style);
				box.setText(title);
				box.setMessage(prompt != null ? msg + "\n\n"+prompt : msg);
				isOpenDialog = true;
				int open = box.open();
				isOpenDialog = false;
				if (disposeDialog.get()) shell.dispose();
				close.accept(open);
			} else {
				headlessDialog(close);
			}
		});
	}

	private void headlessDialog(Consumer<Integer> close) {
		@SuppressWarnings("unchecked")
		CompletableFuture<Integer> f = (CompletableFuture<Integer>) chromium.getData("chromium.dialogs");
		if (f != null) {
			f.thenAccept(close);
			chromium.setData("chromium.dialogs", null);
			while (!f.isDone()) {
				if (!chromium.getDisplay().readAndDispatch()) chromium.getDisplay().sleep();
			}
		}
	}

	private void onDialogClosed() {
		debug("on_dialog_closed_cb disposing: " + disposing);
		if (disposing == Dispose.Unload) {
			disposing = Dispose.UnloadClosed;
		}
	}
	
	private boolean onBeforeUnloadDialog(String message_text, boolean is_reload, CefJSDialogCallback callback) {
		debug("on_before_unload_dialog disposing: " + disposing);
		if (disposing == Dispose.FromClose) {
			disposing = Dispose.Unload;
		}	
		if (useSwtDialogs()) {
			openJsDialog(false, JSDialogType.JSDIALOGTYPE_CONFIRM, "Are you sure you want to leave this page?", message_text, null, null, callback);
			if (disposing == Dispose.Unload) {
				disposing = Dispose.UnloadClosed;
			}
			return true;
		}
		return false;
	}

	private static boolean useSwtDialogs() {
		return "gtk".equals(SWT.getPlatform()) || !"native".equals(System.getProperty("chromium.dialogs", "swt"));
	}

	private static void setContextMenuHandler() {
		contextMenuHandler = new ContextMenuHandler();
		clientHandler.addContextMenuHandler(contextMenuHandler);
	}
	
	static class ContextMenuHandler extends CefContextMenuHandlerAdapter {
		public void onBeforeContextMenu(CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
			debug("onBeforeContextMenu", browser);
			if (!browser.isPopup()) {
				getChromium(browser).onBeforeContextMenu(model);
			}
		};
	}
	
	private void onBeforeContextMenu(CefMenuModel model) {
		syncExec(() -> {
			if (chromium.getMenu() != null) {
				model.clear();
				chromium.getMenu().setVisible(true);
			}
		});
	}

	private static void setDownloadHandler() {
		downloadHandler = new DownloadHandler();
		clientHandler.addDownloadHandler(downloadHandler);
	}
	
	static class DownloadHandler extends CefDownloadHandlerAdapter {
		@Override
		public void onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem, String suggestedName,
				CefBeforeDownloadCallback callback) {
			debug("onBeforeDownload", browser);
			Chromium chm = getChromium(browser);
			if (chm != null) chm.onBeforeDownload(downloadItem, suggestedName, callback);
		}

		@Override
		public void onDownloadUpdated(CefBrowser browser, CefDownloadItem downloadItem,
				CefDownloadItemCallback callback) {
			debug("onDownloadUpdated", browser);
			Chromium.onDownloadUpdated(browser.getIdentifier(), downloadItem, callback);
		}
	}
	
	private void onBeforeDownload(CefDownloadItem download_item, String suggested_name, CefBeforeDownloadCallback callback) {
		String name = suggested_name;

		DownloadItem downloadItem = new DownloadItem(download_item);

		if (Boolean.getBoolean("chromium.downloadLocationListener")) {
			LocationEvent event = new LocationEvent(chromium);
			event.display = chromium.getDisplay();
			event.widget = chromium;
			event.doit = true;
			event.location = getPlainUrl(downloadItem.url);
			for (LocationListener listener : locationListeners) {
				listener.changing(event);
			}
			if (!event.doit) {
				return;
			}
		}

		asyncExec(() -> {
			if ("test".equals(System.getProperty("chromium.dialogs", ""))) {
				Consumer<Integer> close = open -> {
					boolean r = open == SWT.OK || open == SWT.YES ? true : false;
					debug("JS Dialog Closed with " + r);
					if (r) {
						if (downloadItem.isValid) {
							Download d = new Download(downloadItem, name, this);
							d.isReady = true;
						}
						callback.Continue("" , false);
					}
				};
				headlessDialog(close);
				return;
			}
			if ("gtk".equals(SWT.getPlatform())) {
				// Dont open multiple donwload dialogs. Prevent block ui.
				if (isOpenDialog) {
					Download d = new Download(downloadItem, null, this);
					d.isReady = true;
					return;
				}
				
				AtomicBoolean disposeDialog = new AtomicBoolean(false);
				Shell shell = getShell((chromium != null && !chromium.isDisposed()), disposeDialog);
				FileDialog dlg = new FileDialog(shell, SWT.SAVE | SWT.TOP);
				dlg.setText("Save File");
				dlg.setOverwrite(true);
				if (name != null && !name.isEmpty()) {
					dlg.setFileName(name);
				}
				
				Thread thread = preventBlockUI();
				
				asyncExec(() -> {
					isOpenDialog = true;
					thread.start();
					String selected = dlg.open();
					isOpenDialog = false;
					if (disposeDialog.get()) shell.dispose();
					if (selected != null) {
						if (downloadItem.isValid) {
							Download d = new Download(downloadItem, selected, this);
							d.isReady = true;
							callback.Continue(selected, false);
							return;
						}
					}
					Download d = new Download(downloadItem, null, this);
					d.isReady = true;
				});
			} else {
				if (downloadItem.isValid) {
					new Download(downloadItem, name, this);
				}
				callback.Continue("", true);
			}
		});
	}

	private Thread preventBlockUI() {
		// This method is used for prevent block ui when open dialog
		Thread thread = new Thread(() -> {
			while (isOpenDialog) {
				try {
					Thread.sleep(100);
					Display.getDefault().asyncExec(null);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		return thread;
	}

	private static void onDownloadUpdated(int browserId, CefDownloadItem download_item, CefDownloadItemCallback callback) {
		if (download_item.isValid()) {
			asyncExec(() -> {
				Download download = Download.downloads.get(download_item.getId());
				if (download != null && download.isReady) {
					download.update(download_item, callback);
					if (download_item.isCanceled() || !download.isReady) {
						download.dispose();
					}
					else if (!download.open && !Boolean.getBoolean("chromium.disable-download-progress")) {
						download.show();
					}
				}
			});
		}
	}

	static class Download {
		static final Map<Integer, Download> downloads = new HashMap<>();
		private int id;
		private Shell shell = null;
		private Label statusLabel;
		private Button cancel;
		private CefDownloadItemCallback cancel_cb;
		private boolean open;
		private Label nameLabel;
		private ProgressBar pb;
		final Listener cancelListener = event -> {
			pb.setState(SWT.PAUSED);
			cancel_cb.cancel();
			dispose();
		};
		public boolean isReady = false;

		public Download(DownloadItem download_item, String name, Chromium browser) {
			Download.downloads.put(download_item.id, this);
			// Create empty download for cancel on update
			if (name == null) {
				return;
			}
			this.id = download_item.id;
			shell = new Shell();
			String msg = Compatibility.getMessage ("SWT_FileDownload"); //$NON-NLS-1$
			shell.setText (msg);
			GridLayout gridLayout = new GridLayout ();
			gridLayout.marginHeight = 15;
			gridLayout.marginWidth = 15;
			gridLayout.verticalSpacing = 20;
			shell.setLayout (gridLayout);
			
			nameLabel = new Label (shell, SWT.WRAP);
			setText(download_item);
			GridData data = new GridData ();
			Monitor monitor = browser.chromium != null ? browser.chromium.getMonitor () : Display.getDefault().getPrimaryMonitor();
			int maxWidth = monitor.getBounds ().width / 2;
			int width = nameLabel.computeSize (SWT.DEFAULT, SWT.DEFAULT).x;
			data.widthHint = Math.min (width, maxWidth);
			data.horizontalAlignment = GridData.FILL;
			data.grabExcessHorizontalSpace = true;
			nameLabel.setLayoutData (data);
			
			int pbStyle = download_item.percentComplete == -1 ? SWT.SMOOTH | SWT.HORIZONTAL | SWT.INDETERMINATE : SWT.SMOOTH | SWT.HORIZONTAL;
			pb = new ProgressBar(shell, pbStyle);
			pb.setSelection(download_item.percentComplete);
			pb.setMinimum(0);
			pb.setMaximum(100);
			pb.setState(SWT.NORMAL);
			pb.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			
			statusLabel = new Label (shell, SWT.NONE);
			statusLabel.setText (Compatibility.getMessage ("SWT_Download_Started")); //$NON-NLS-1$
			data = new GridData (GridData.FILL_BOTH);
			statusLabel.setLayoutData (data);
			
			cancel = new Button (shell, SWT.PUSH);
			cancel.setText (Compatibility.getMessage ("SWT_Cancel")); //$NON-NLS-1$
			data = new GridData ();
			data.horizontalAlignment = GridData.CENTER;
			cancel.setLayoutData (data);
			cancel.addListener (SWT.Selection, cancelListener);
		}

		private void show() {
			open = true;
			shell.pack ();
			shell.open ();
		}

		private void update(CefDownloadItem download_item, CefDownloadItemCallback callback) {
			// Cancel download when cancel folder dialog
			if (shell == null) {
				callback.cancel();
				isReady = false;
				return;
			}
			this.cancel_cb = callback;
			if (shell.isDisposed()) {
				dispose();
				return;
			}
			if (download_item.isComplete()) {
				Display.getDefault().timerExec(3000, this::dispose);
			}
			
			setText(new DownloadItem(download_item));
			
			long current = download_item.getReceivedBytes() / 1024L;
			long total = download_item.getTotalBytes() / 1024L;
			String message = Compatibility.getMessage ("SWT_Download_Status", new Object[] {Long.valueOf(current), Long.valueOf(total)}); //$NON-NLS-1$
			message = message + " at " + (download_item.getCurrentSpeed() / 1024L) + " KB/s " + (download_item.getPercentComplete() != -1 ? "("+download_item.getPercentComplete()+"%)" : "");
			statusLabel.setText (message);
			pb.setSelection(download_item.getPercentComplete());
			
			if (download_item.isCanceled() || (!download_item.isComplete() && !download_item.isInProgress())) { // error or cancel
				pb.setState(SWT.ERROR);
				statusLabel.setText (Compatibility.getMessage ("SWT_Download_Error")); //$NON-NLS-1$
				cancel.removeListener (SWT.Selection, cancelListener);
				cancel.addListener (SWT.Selection, event -> dispose());
				return;
			}
		}

		private void setText(DownloadItem download_item) {
			String nameString = download_item.fullPath;
			String urlString = download_item.url;
			String msg = Compatibility.getMessage ("SWT_Download_Location", new Object[] {nameString +"\n", urlString}); //$NON-NLS-1$
			if (urlString.startsWith("data:")) {
				msg = "Saving...";
			}
			nameLabel.setText (msg);
		}

		private void dispose() {
			Download.downloads.remove(id);
			cancel_cb = null;
			
			if (shell != null) shell.dispose();
		}
	}

	private static void setKeyboardHandler(){
		keyboardHandler = new KeyboardHandler();
		clientHandler.addKeyboardHandler(keyboardHandler);
	}
	
	static class KeyboardHandler extends CefKeyboardHandlerAdapter {
		@Override
		public boolean onKeyEvent(CefBrowser browser, CefKeyEvent event) {
			if (!browser.isPopup()) {
				return getChromium(browser).on_key_event(event);
			}
			return false;
		}
	}

	private enum cef_event_flags {
		NONE(0),
		CAPS_LOCK_ON(1),
		SHIFT_DOWN(2, SWT.SHIFT),
		CONTROL_DOWN(4, SWT.CONTROL),
		ALT_DOWN(8, SWT.ALT),
		/// Mac OS-X command key.
		COMMAND_DOWN(128, SWT.COMMAND),
		/// Mac OS-X command key.
		NUM_LOCK_ON(256),
		/// Mac OS-X command key.
		IS_KEY_PAD(512),
		/// Mac OS-X command key.
		IS_LEFT(1024),
		/// Mac OS-X command key.
		IS_RIGHT(2048);
		
		int cefFlag;
		private int swtKey;

		private cef_event_flags(int flag) {
			this(flag, SWT.NONE);
		}
		private cef_event_flags(int flag, int swtKey) {
			this.cefFlag = flag;
			this.swtKey = swtKey;
		}
		
		boolean shouldSetState(int modifiers, Event event) {
			if (isSet(modifiers)) {
				if (event.type == SWT.KeyDown && event.keyCode == swtKey && swtKey != SWT.NONE) {
					return false;
				}
				return true;
			}
			else if (event.type == SWT.KeyUp && event.keyCode == swtKey && swtKey != SWT.NONE) {
				return true;
			}
			return false;
		}
		
		boolean isSet(int modifiers) {
			return (modifiers & cefFlag) != 0;
		}
	}
	
	private boolean on_key_event(CefKeyEvent event) {
		if (isDisposed()) return false;

		Event firedEvent = new Event();
		if (event.type == EventType.KEYEVENT_KEYDOWN || event.type == EventType.KEYEVENT_RAWKEYDOWN) {
			firedEvent.type = SWT.KeyDown;
		} else if (event.type == EventType.KEYEVENT_KEYUP) {
			firedEvent.type = SWT.KeyUp;
		} else {
			return false;
		}

		int translateKey = event.windows_key_code == 91 || event.windows_key_code == 93 ? SWT.COMMAND : translateKey(event.windows_key_code);
		firedEvent.keyCode = translateKey != 0 ? translateKey : event.windows_key_code /*+ 32*/;
		firedEvent.widget = chromium;
		firedEvent.character = event.character;
		firedEvent.display = chromium.getDisplay();
		int stateMask = 0;
		if (cef_event_flags.CAPS_LOCK_ON.shouldSetState(event.modifiers, firedEvent)){
			stateMask += SWT.CAPS_LOCK;
		}
		if (cef_event_flags.SHIFT_DOWN.shouldSetState(event.modifiers, firedEvent)) {
			stateMask += SWT.SHIFT;
		}
		if (cef_event_flags.CONTROL_DOWN.shouldSetState(event.modifiers, firedEvent)) {
			stateMask += SWT.CTRL;
		}
		if (cef_event_flags.ALT_DOWN.shouldSetState(event.modifiers, firedEvent)) {
			stateMask += SWT.ALT;
		}
		if (cef_event_flags.COMMAND_DOWN.shouldSetState(event.modifiers, firedEvent)) {
			stateMask += SWT.COMMAND;
		}
		if (cef_event_flags.NUM_LOCK_ON.shouldSetState(event.modifiers, firedEvent)) {
			stateMask += SWT.NUM_LOCK;
		}
		if (cef_event_flags.IS_KEY_PAD.shouldSetState(event.modifiers, firedEvent)) {
			stateMask += SWT.KEYPAD;
		}
		if (cef_event_flags.IS_LEFT.shouldSetState(event.modifiers, firedEvent)) {
			stateMask += SWT.LEFT;
		}
		if (cef_event_flags.IS_RIGHT.shouldSetState(event.modifiers, firedEvent)) {
			stateMask += SWT.RIGHT;
		}
		firedEvent.stateMask = stateMask;
		
//		System.out.println("e.windows_key_code: " + e.windows_key_code + " translated: "+ translateKey(e.windows_key_code));
//		System.out.println("e.native_key_code: " + e.native_key_code + " translated: "+ translateKey(e.native_key_code));
//		System.out.println("e.character: " + e.character + " e.unmodified_character: " + e.unmodified_character);
		
		asyncExec(() -> {
			if (!isDisposed()) {
				sendKeyEvent(firedEvent);
			}
		});
		
		if (firedEvent.doit && "cocoa".equals(SWT.getPlatform())) {
			CefFrame focusedFrame = cefBrowser.getFocusedFrame();
			CefFrame cefFrame = focusedFrame != null ? focusedFrame : cefBrowser.getMainFrame();
			if (event.modifiers == 128 && event.type != EventType.KEYEVENT_RAWKEYDOWN) {
				if (event.native_key_code >= 8 && event.native_key_code <= 9) {
					return true;
				}
			} else if (event.modifiers == 128 && event.type == EventType.KEYEVENT_RAWKEYDOWN) {
				switch (event.native_key_code) {
				// Select all: command + a
				case 0:
					cefFrame.selectAll();
					return true;
				// Copy: command + c
				case 8:
					cefFrame.copy();
					return true;
				// Paste: command + v
				case 9:
					cefFrame.paste();
					CefApp.getInstance().doMessageLoopWork(-1);
					return true;
				// Cut: command + x
				case 7:
					cefFrame.cut();
					return true;
				// Undo: command + z
				case 6:
					cefFrame.undo();
					CefApp.getInstance().doMessageLoopWork(-1);
					return true;
				default:
					return false;
				}
			} else if (event.modifiers == 130 && event.type == EventType.KEYEVENT_RAWKEYDOWN) {
				// Redo: command + shift + z
				if (event.native_key_code == 6) {
					cefFrame.redo();
					CefApp.getInstance().doMessageLoopWork(-1);
					return true;
				}
			}
		}
		//Exit fullSreen
		if (fullscreenListener != null && event.windows_key_code == 27
				&& event.type == EventType.KEYEVENT_RAWKEYDOWN) {
			syncExec(() -> cefBrowser.getComposite().getShell().setFullScreen(false));
			execute("document.exitFullscreen()");
		}
		return false;
	}

	private static void setFocusHandler() {
		focusHandler = new FocusHandler();
		clientHandler.addFocusHandler(focusHandler);
	}
	
	static class FocusHandler extends CefFocusHandlerAdapter {
		@Override
		public void onTakeFocus(CefBrowser browser, boolean next) {
			debug("onTakeFocus", browser);
			if (!browser.isPopup()) {
				getChromium(browser).onTakeFocus(next);
			}
		}

		@Override
		public boolean onSetFocus(CefBrowser browser, FocusSource source) {
			debug("onSetFocus", browser);
			if (!browser.isPopup()) {
				return getChromium(browser).onSetFocus(source);
			}
			return false;
		}

		@Override
		public void onGotFocus(CefBrowser browser) {
			debug("onGotFocus", browser);
			if (!browser.isPopup()) {
				getChromium(browser).onGotFocus();
			}
		}
	}
	
	private void onGotFocus() {
		syncExec(() -> {
			if (isDisposed())
				return;
			if (chromium.getDisplay().getFocusControl() != chromium && focusListener != null) {
				debugPrint("on_got_focus: setFocus");
				boolean focusListenerPrev = focusListener.enabled;
				focusListener.enabled = false;
				chromium.setFocus();
				focusListener.enabled = focusListenerPrev;
//				browserFocus(true);
			}
			else if ("gtk".equals(SWT.getPlatform())) {
				// debugPrint("on_got_focus: gtk force focus");
//				browserFocus(true);
			}
			if ("win32".equals(SWT.getPlatform()) && focusListener != null) // consider this for all os
				focusListener.enabled = true;
			hasFocus = true;
		});
	}

	private boolean onSetFocus(FocusSource focusSource) {
		if (isDisposed()) return true;
		boolean chromiumFocusControl = "gtk".equals(SWT.getPlatform()) ? true : syncExec(() -> chromium.getDisplay().getFocusControl()) != chromium;
		if (ignoreFirstFocus && chromiumFocusControl && focusSource == FocusSource.FOCUS_SOURCE_NAVIGATION) {
			debugPrint("ignoreFirstFocus");
			ignoreFirstFocus = false;
			return true;
		}

		if (syncExec(() -> isDisposed() ? true : chromium.isFocusControl())) {
			if (focusListener != null)
				focusListener.enabled = true;
		} else {
			if (focusListener != null && !"gtk".equals(SWT.getPlatform()))
				focusListener.enabled = false;
			return true;
		}

		hasFocus = true;
		return false;
	}

	private Composite getParent(Composite composite) {
		Composite parent = composite;
		while (parent.getParent() != null) {
			parent = parent.getParent();
		}
		return parent;
	}

	private List<Control> getTabList(Composite composite) {
		List<Control> tabOrder = new ArrayList<Control>();

		for (Control c: composite.getTabList()) {
			if (c.isVisible() && !(c instanceof Label)) {
				if (c instanceof Composite && ((Composite) c).getTabList().length > 0) {
					tabOrder.addAll(getTabList((Composite)c));
				} else {
					tabOrder.add(c);
				}
			}
		}
		return tabOrder;
	}

	private void onTakeFocus(boolean next) {
		if (isDisposed()) return;
		hasFocus = false;
		syncExec(() -> {
			List<Control> tabOrder = getTabList(getParent(chromium));
			int indexOf = tabOrder.indexOf(chromium);
			if (indexOf != -1) {
				int newIndex = (next) ? indexOf + 1 : indexOf - 1;
				if (newIndex > 0 && newIndex < tabOrder.size() && !tabOrder.get(newIndex).isDisposed()) {
					tabOrder.get(newIndex).setFocus();
					return;
				}
			}
			Control nextFocus = tabOrder.get(0);
			if (!isDisposed() && !chromium.getParent().isDisposed() && !nextFocus.isDisposed()) {
				nextFocus.setFocus();
			}
		});
	}

	@Override
	public boolean isFocusControl() {
//		debugPrint("isFocusControl: "+ hasFocus + " - "+Display.getDefault().getFocusControl());
		return ("gtk".equals(SWT.getPlatform())) ? hasFocus : false;
	}
	
	private synchronized void checkBrowser() {
		if (cefBrowser == null) {
			SWT.error(SWT.ERROR_WIDGET_DISPOSED);
		}
	}

//	protected void browserFocus(boolean set) {
//		debugPrint("cef focus: " + set);
//		if (!isDisposed() && browser != 0) {
//			long /*int*/ parent = (Display.getDefault().getActiveShell() == null) ? 0 : getHandle(chromium.getParent());
//			if (chromium.getDisplay().getActiveShell() != chromium.getShell()) {
////				System.err.println("Ignore do_message_loop_work due inactive shell");
//				return;
//			}
////			ChromiumLib.cefswt_set_focus(browser, set, parent);
//		}
//	}
	
	@Override
	public boolean close() {
		if (disposing != Dispose.No || isDisposed())
			return false;
		if (cefBrowser == null)
			return true;
		boolean closed = false;
		debugPrint("call try_close_browser");
		disposing = Dispose.FromClose;
		cefBrowser.close(false);
		
		long t = System.currentTimeMillis();
		long end = t+10000;
		Shell shell = chromium.getShell();
		Display display = shell.getDisplay();
		while (!shell.isDisposed() && System.currentTimeMillis() < end) {
			//debug("in close, disposing:"+disposing);
			if (disposing == Dispose.Unload) {
				//debug("in close, disposing:"+disposing);
				end += 1000;
			}
			else if (disposing == Dispose.UnloadCancel) {
				debug("in close, disposing:"+disposing);
				break;
			}
			else if (disposing == Dispose.UnloadClosed) {
				debug("in close, disposing:"+disposing);
				disposing = Dispose.WaitIfClosed;
				end = System.currentTimeMillis() + 600;
			}
			else if (disposing == Dispose.DoIt) {
				debug("in close, disposing:"+disposing);
				closed = true;
				break;
			}
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		if (!closed) {
			disposing = Dispose.No;
		}
		//debugPrint("try_close_browser returned");
		return closed;
	}

	public void dispose() {
		debugPrint("in dispose, disposing "+ disposing);
		if (created != null && !created.isDone() && cefBrowser != null) {
			cefBrowser.close(false);
			Shell shell = chromium.getShell();
			Display display = shell.getDisplay();
			while (!shell.isDisposed() && !created.isDone()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		}
		if (disposing == Dispose.FromDispose || isDisposed())
			return;
		if (disposing != Dispose.FromBrowser)
			disposing = Dispose.FromDispose;
//		disposingAny++;
		if (focusListener != null)
			chromium.removeFocusListener(focusListener);
		focusListener = null;
		if (traverseListener != null) {
			chromium.removeListener(SWT.Traverse, traverseListener);
			chromium.removeListener(SWT.KeyDown, traverseListener);
			traverseListener = null;
		}
		if (resize != null) {
			resize.remove();
		}
	}

	private static void setupCookies() {
		WebBrowser.NativeClearSessions = () -> {
			Engine.ready.thenRun(() -> {
				CefCookieManager globalManager = CefCookieManager.getGlobalManager();
				if (globalManager != null) {
					globalManager.deleteCookies(null, null);
				}
				CefCompletionCallback callback = new CefCompletionCallback() {
					
					@Override
					public void onComplete() {
						debug("certificate exceptions cleared");
					}
				};
				CefRequestContext.getGlobalContext().clearCertificateExceptions(callback);
			});
		};
		WebBrowser.NativeClearCookies = () -> {
			Engine.ready.thenRun(() -> {
				CefCookieManager globalManager = CefCookieManager.getGlobalManager();
				if (globalManager != null) {
					CefCookieVisitor visitor = new CefCookieVisitor() {
						@Override
						public boolean visit(CefCookie cookie, int count, int total, BoolRef delete) {
							
							if (cookie.domain.matches(WebBrowser.ClearCookieUrl)
									&& cookie.name.matches(WebBrowser.ClearCookieName)) {
								debug("Delete Cookie: " + cookie.name);
								delete.set(true);
							}
							return true;
						}
					};
					WebBrowser.CookieResult = globalManager.visitAllCookies(visitor);
				}
			});
		};
		WebBrowser.NativeSetCookie = () -> {
			List<HttpCookie> cookies = HttpCookie.parse(WebBrowser.CookieValue);
			for (HttpCookie cookie : cookies) {
				long age = cookie.getMaxAge();
				if (age != -1) {
					age = Instant.now().plusSeconds(age).getEpochSecond();
				}
				Date now = new Date();
				Date expires = new Date(age);
				CefCookie cefCookie = new CefCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(),
						cookie.getPath(), cookie.getSecure(), cookie.isHttpOnly(), now, now, cookie.getMaxAge() != -1,
						expires);
				String cookieUrl = WebBrowser.CookieUrl;
				Engine.ready.thenRun(() -> {
					CefCookieManager globalManager = CefCookieManager.getGlobalManager();
					if (globalManager != null) {
						WebBrowser.CookieResult = globalManager.setCookie(cookieUrl, cefCookie);
//						debug("set cookie: "+ WebBrowser.CookieResult + " " + cookie.getName());
						globalManager.flushStore(null);
					}
				});
				break;
			}
		};
		WebBrowser.NativeGetCookie = () -> {
			AtomicBoolean cookieVisited = new AtomicBoolean();
			CefCookieVisitor visitor = new CefCookieVisitor() {
				@Override
				public boolean visit(CefCookie cookie, int count, int total, BoolRef delete) {
					//debug("Visitor " + count + "/" +total + ": " + cookie.name+ ":" + Thread.currentThread());
					if (WebBrowser.CookieName != null && WebBrowser.CookieName.equals(cookie.name)) {
						debug("cookie value: " + cookie.value);
						WebBrowser.CookieValue = cookie.value;
						cookieVisited.set(true);
						return false;
					}
					else if (count+1 == total) {
						cookieVisited.set(true);
					}
					return true;
				}
			};
			for (int i = 0; i < 2; i++) {
				CefCookieManager globalManager = CefCookieManager.getGlobalManager();
				if (globalManager == null) {
					throw new SWTException("Failed to get cookies, cookie manager not ready");
				}
				boolean result = globalManager.visitUrlCookies(WebBrowser.CookieUrl, true, visitor);
				if (!result) {
					throw new SWTException("Failed to get cookies");
				}
				long end = System.currentTimeMillis()+250;
				Display display = Display.getCurrent();
				while (!cookieVisited.get() && !display.isDisposed() && System.currentTimeMillis() < end) {
//					debug("in cookie loop");
					if (!display.readAndDispatch()) {
						display.sleep();
					}
				}
//				debug("end cookie loop");
				if (cookieVisited.get())
					return;
			}
		};
	}

	private class ResizeListener implements ControlListener, Listener, Runnable {
		private boolean mouseDown;
		private Composite overlay;
		private Image image;
		private Image scaledImage;
		private Runnable loop = new Runnable() {
			@Override
			public void run() {
				CefApp.getInstance().doMessageLoopWork(-1);
			}
		};
		
		public ResizeListener() {
			overlay = new Composite(chromium, SWT.NONE);
			overlay.setBackground(chromium.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			overlay.setBounds(chromium.getClientArea());
			overlay.setVisible(false);
			install();
		}
	
		private Image getImage() {
			Rectangle bounds = overlay.getBounds();
			Display display = chromium.getDisplay();
			if (bounds.width <= 0 || bounds.height <= 0)
				return null;
			if (!created.isDone() || !chromium.isVisible())
				return null;
			if (image == null) {
				GC gc = new GC(chromium.getShell());
				Rectangle cefBounds = cefBrowser.getCurrentBounds();
				if (cefBounds == null || cefBounds.width == 0 || cefBounds.height == 0)
					return null;
				Image orig = new Image(display, cefBounds.width, cefBounds.height);
				gc.copyArea(orig, cefBounds.x, cefBounds.y);
				gc.dispose();
				image = orig;
			}
			if (bounds.equals(image.getBounds()))
				return image;
			if (scaledImage != null) {
				if (scaledImage.getBounds().equals(bounds)) {
					return scaledImage;
				}
				scaledImage.dispose();
				scaledImage = null;
			}
			scaledImage = new Image(display, bounds.width, bounds.height);
			GC gc = new GC(scaledImage);
			gc.setAntialias(SWT.ON);
			gc.setInterpolation(SWT.HIGH);
			gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 0, 0, bounds.width, bounds.height);
			gc.dispose();
			return scaledImage;
		}
	
		@Override
		public void run() {
			hideOverlay();
		}
	
		@Override
		public void controlResized(ControlEvent e) {
			if (isDisposed()) return;
			overlay.setBounds(chromium.getClientArea());
			showOverlay(e.display);
		}
	
		@Override
		public void controlMoved(ControlEvent e) {
			if (isDisposed()) return;
			overlay.setBounds(chromium.getClientArea());
			showOverlay(e.display);
		}
		
		@Override
		public void handleEvent(Event event) {
			if (isDisposed()) return;
			mouseDown = event.type == SWT.MouseDown;
			if (mouseDown && chromium.isVisible() && isSash(event.widget)) {
				getImage();
			}
			if (event.type == SWT.DragDetect && chromium.isVisible() && (event.widget instanceof CTabFolder || event.widget instanceof TabFolder)) {
				getImage();
				event.display.timerExec(8000, this::disposeImage);
			} else if (!mouseDown && (image != null || scaledImage != null)) {
				disposeImage();
			}
		}
	
		private void showOverlay(Display display) {
			if (created.isDone()) {
				overlay.setVisible(true);
				Image img = getImage();
				overlay.setBackgroundImage(img);
				if (img == null)
					overlay.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
				display.timerExec(33*1, loop);
				display.timerExec(33*4, this);
			}
		}
	
		private void hideOverlay() {
			if (overlay.isVisible())
				overlay.setVisible(false);
			disposeImage();
		}
	
		private void disposeImage() {
			if (image != null) {
				image.dispose();
				image = null;
			}
			if (scaledImage != null) {
				scaledImage.dispose();
				scaledImage = null;
			}
		}
	
		private boolean isSash(Widget widget) {
			if (widget instanceof Sash && isAncestor(((Sash) widget).getParent()))
				return true;
			if (!(widget instanceof Composite))
				return false;
			Layout layout = ((Composite) widget).getLayout();
			if (layout != null && layout.getClass().getSimpleName().contains("SashLayout") && isAncestor((Composite) widget))
				return true;
			return false;
		}
	
		private boolean isAncestor(Composite ancestor) {
			Composite parent = chromium.getParent();
			while (parent != null) {
				if (parent == ancestor)
					return true;
				parent = parent.getParent();
			}
			return false;
		}
	
		public void remove() {
			chromium.removeControlListener(this);
			chromium.getDisplay().removeFilter(SWT.KeyDown, this);
			chromium.getDisplay().removeFilter(SWT.KeyUp, this);
			chromium.getDisplay().removeFilter(SWT.DragDetect, this);
			resize = null;
		}
	
		public void install() {
			chromium.addControlListener(this);
			chromium.getDisplay().addFilter(SWT.MouseDown, this);
			chromium.getDisplay().addFilter(SWT.MouseUp, this);
			chromium.getDisplay().addFilter(SWT.DragDetect, this);
		}
	}

	private final class CefFocusListener implements FocusListener {
		private boolean enabled = true;
		
		@Override
		public void focusLost(FocusEvent e) {
			//debugPrint("focusLost "+enabled);
			if (!enabled)
				return;
			enabled = false;
//			browserFocus(false);
			// System.out.println(Display.getDefault().getFocusControl());
			if (cefBrowser != null)
				cefBrowser.setFocus(false);
			hasFocus = false;
			enabled = true;
		}

		@Override
		public void focusGained(FocusEvent e) {
			//debugPrint("focusGained "+enabled);
			if (!enabled)
				return;
			enabled = false;
//				browserFocus(true);
			if (cefBrowser != null && !isOpenDialog) {
				cefBrowser.setFocus(true);
			}
			enabled = true;
		}
	}
	
	@Override
	public boolean back() {
		if (cefBrowser != null && cefBrowser.canGoBack()) {
			cefBrowser.goBack();
			return true;
		}
		return false;
	}

	@Override
	public boolean forward() {
		if (cefBrowser != null && cefBrowser.canGoForward()) {
			cefBrowser.goForward();
			return true;
		}
		return false;
	}

	@Override
	public boolean execute(String script) {
		if (!jsEnabled) {
			return false;
		}
		enableProgress.thenRun(() -> {
			cefBrowser.executeJavaScript(script, getPlainUrl(url), 1);
		});
		return true;
	}
	
	@Override
	public Object evaluate(String script) throws SWTException {
		if (!jsEnabled) {
			return null;
		}
		if (cefBrowser == null) {
			if (paintListener != null) {
				if (!Boolean.getBoolean("chromium.headless"))
					chromium.removePaintListener(paintListener);
				paintListener = null;
				createBrowser();
			}
		}
		checkBrowser();
		AbstractEval eval = null;
		try {
			if (!functionsResourceHandler.isEmpty() && functionsResourceHandler.peek().inFunction) {
				eval = new EvalBrowserFunctionImpl(router, functionsResourceHandler.peek());
			} else if (inEvalBlocking) {
				eval = new EvalFileImpl(cefBrowser);
			} else {
				eval = new EvalSimpleImpl(cefBrowser, router, getPlainUrl(url));
			}
			return eval.eval(script, created);
		} catch (InterruptedException e) {
			throw new SWTException("Script that was evaluated failed");
		} catch (ExecutionException e) {
			throw (SWTException) e.getCause();
		}
	}

	private String encodeType(Object ret) {
		try {
			return Jsoner.serialize(ret);
		} catch(IllegalArgumentException e) {
			throw new SWTException(new SWTException(SWT.ERROR_INVALID_RETURN_VALUE).getMessage() + ": " + ret.getClass().getName());
		}
	}

	@Override
	public String getBrowserType() {
		return "chromium";
	}

	@Override
	public String getText() {
		if (cefBrowser != null && !isDisposed() && disposing == Dispose.No) {
			CompletableFuture<String> textVisited = new CompletableFuture<>();
			CefStringVisitor visitor = new CefStringVisitor() {
				
				@Override
				public void visit(String string) {
					if (string != null) {
						debugPrint("text visited completed");
						textVisited.complete(string);
					} else {
						textVisited.complete("");
						debugPrint("text visited null");
					}
				}
			};
			cefBrowser.getSource(visitor);
			
			Display display = chromium.getDisplay();
			while (!textVisited.isDone() && !display.isDisposed()) {
				debug("in text loop");
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			try {
				return textVisited.get();
			} catch (InterruptedException | ExecutionException e) {
			}
		}
		return "";
	}
	
	@Override
	public String getUrl() {
		if (cefBrowser == null) {
			if (this.url == null) {
				return "about:blank";
			}
			return getPlainUrl(this.url);
		}
		String cefurl = cefBrowser.getURL();
		if (cefurl == null)
			cefurl = getPlainUrl(this.url);
		return cefurl;
	}

	@Override
	public boolean isBackEnabled() {
		if (cefBrowser == null) {
			return false;
		}
		return cefBrowser.canGoBack();
	}

	@Override
	public boolean isForwardEnabled() {
		if (cefBrowser == null) {
			return false;
		}
		return cefBrowser.canGoForward();
	}
	
	@Override
	public Object getWebBrowser() {
		return extraApi();
	}

	private ExtraApi extraApi() {
		if (extraApi == null) {
			extraApi = new ExtraApi();
		}
		return extraApi;
	}

	@Override
	public void refresh() {
		jsEnabled = jsEnabledOnNextPage;
		if (cefBrowser != null) {
			cefBrowser.reload();
		}
	}

	@Override
	public boolean setText(String html, boolean trusted) {
		String texturl = DATA_TEXT_URL + Base64.getEncoder().encodeToString(html.getBytes());
		return setUrl(texturl, null, null);
	}
	
	private static String getPlainUrl(String url) {
		if (url != null && url.startsWith(DATA_TEXT_URL)) {
			return url.substring(0, DATA_TEXT_URL.length()-8);
		}
		return url;
	}

	@Override
	public boolean setUrl(String url, String postData, String[] headers) {
		// if not yet created will be used when created
		boolean isPost = postData != null || headers != null;
		boolean isCreating = clientHandler != null && clientHandler.isCreating();
		if (created.isDone() || (!isPost && !isCreating)) {
			this.url = url;
		}
		jsEnabled = jsEnabledOnNextPage;
		debugPrint("set url: " + getPlainUrl(url));
		if (created.isDone() || isCreating || isPost) {
			ignoreFirstFocus = true;
			urls = enableProgress.runAfterBoth(urls, () -> {
				Chromium.this.url = url;
				doSetUrl(url, postData, headers);
			});
		}
		return true;
	}

	private void doSetUrl(String url, String postData, String[] headers) {
		loaded = new CompletableFuture<>();
		Net.config();
		if (postData == null && headers == null) {
			cefBrowser.loadURL(url);
		} else {
			CefRequest request = CefRequest.create();
			request.setURL(url);
			if (postData != null) {
				CefPostData post = CefPostData.create();
				CefPostDataElement elem = CefPostDataElement.create();
				byte[] postBytes = postData.getBytes(Charset.forName("ASCII"));
				elem.setToBytes(postBytes.length, postBytes);
				post.addElement(elem);
				request.setPostData(post);
			}
			if (headers != null) {
				Map<String, String> headersMap = Arrays.stream(headers).map(h -> h.split(":", 2))
						.collect(Collectors.toMap(k -> k[0], v -> v[1]));
				request.setHeaderMap(headersMap);
			}
			cefBrowser.loadRequest(request);
		}
	}
	
	@Override
	public void stop() {
		if (cefBrowser != null) {
			loaded.complete(true);
			cefBrowser.stopLoad();
		}
	}
	
	boolean isDisposed() {
		return chromium == null || chromium.isDisposed();
	}

	void syncExec(Runnable run) {
		if (Display.getCurrent() != null) {
			run.run();
		} else {
			Display.getDefault().syncExec(run);
		}
	}

	<V> V syncExec(Callable<V> run) {
		ErrorHandlingCallable<V> handling = new ErrorHandlingCallable<V>(run);
		if (Display.getCurrent() != null) {
			handling.run();
		} else {
			Display.getDefault().syncExec(handling);
		}
		if (handling.exception != null)
			throw new RuntimeException(handling.exception);
		return handling.result;
	}
	
	static void asyncExec(Runnable run) {
		Display.getDefault().asyncExec(run);
	}
	
	private static class ErrorHandlingCallable<T> implements Runnable {

		private Callable<T> runnable;

		private T result;

		private Exception exception;

		private ErrorHandlingCallable(Callable<T> runnable) {
			super();
			this.runnable = runnable;
		}

		@Override
		public void run() {
			try {
				result = runnable.call();
			} catch (Exception e) {
				exception = e;
			}
		}
	}
	
	class ExtraApi extends IndependentBrowser implements ChromiumBrowser {
		List<Object> errors;

		@Override
		protected CefBrowser getBrowser() {
			return cefBrowser;
		}
		
		@Override
		protected CefRequestContext createRequestContext() {
			return super.createRequestContext();
		}

		@Override
		public void find(String search, boolean forward, boolean matchCase) {
			checkBrowser();
			super.find(search, forward, matchCase);
		}

		private void initErrors() {
			if (errors == null) {
				errors = new ArrayList<Object>();
			}
		}

		void error(Object status) {
			initErrors();
			errors.add(status);
		}

		public List<Object> getErrors() {
			initErrors();
			return errors;
		}

		public Chromium getChromium() {
			return Chromium.this;
		}

		@Override
		public void zoom(double zoomLevel) {
			checkBrowser();
			super.zoom(zoomLevel);
		}

		@Override
		protected boolean onConsoleMessage(CefBrowser browser, LogSeverity level, String message, String source, int line) {
			return super.onConsoleMessage(browser, level, message, source, line);
		}

		@Override
		protected boolean handleCertificateProperty() {
			return super.handleCertificateProperty();
		}

		@Override
		public void executeJavacript(String script) {
			execute(script);
		}

		@Override
		public boolean setUrl(String url) {
			return Chromium.this.setUrl(url, null, null);
		}

		@Override
		public Object evaluate(String script) {
			return Chromium.this.evaluate(script);
		}

		@Override
		public boolean close() {
			return Chromium.this.close();
		}
	}
	
	private static void createClipboardRouters() {
		CefMessageRouter writeText = CefMessageRouter.create(new CefMessageRouterConfig("__writeText", "__writeTextCancel"));
		CefMessageRouter readText = CefMessageRouter.create(new CefMessageRouterConfig("__readText", "__readTextCancel"));
		CefMessageRouterHandlerAdapter writeTextHandler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
				Display display = Display.getDefault();
				asyncExec(() -> {
					final Clipboard cb = new Clipboard(display);
					try {
						if (request == null || request.isEmpty()) {
							cb.clearContents();
						}
						else {
							Transfer transfer = TextTransfer.getInstance();
							cb.setContents(new Object[] { request }, new Transfer[] { transfer });
						}	
						callback.success(request);
					} catch (Exception e) {
						e.printStackTrace();
						callback.failure(0, e.getMessage());
					}
					cb.dispose();
				});	
				return true;
			}
		};
		CefMessageRouterHandlerAdapter readTextHandler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
				Display display = Display.getDefault();
				asyncExec(() -> {
					final Clipboard cb = new Clipboard(display);
					TextTransfer transfer = TextTransfer.getInstance();
					try {
						String text = (String) cb.getContents(transfer);
						callback.success(text);
					} catch (Exception e) {
						e.printStackTrace();
						callback.failure(0, e.getMessage());
					}
					cb.dispose();
				});	
				return true;	
			}
		};
		writeText.addHandler(writeTextHandler, true);
		readText.addHandler(readTextHandler, true);
		clientHandler.addMessageRouter(writeText);
		clientHandler.addMessageRouter(readText);
	};
}