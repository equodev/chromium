package com.equo.chromium.swt.internal;

import static com.equo.chromium.swt.internal.Engine.debug;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.cef.CefApp;
import org.cef.CefClientSwt;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserSwt;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.browser.CefPopup;
import org.cef.callback.CefAuthCallback;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefCallback;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefCookieVisitor;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.callback.CefFileDialogCallback;
import org.cef.callback.CefJSDialogCallback;
import org.cef.callback.CefMenuModel;
import org.cef.callback.CefRequestCallback;
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
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.equo.chromium.swt.Browser;
import com.equo.chromium.swt.BrowserFunction;
import com.equo.chromium.swt.ChromiumBrowser;
import com.equo.chromium.swt.ChromiumBrowser.ConsoleListener;
import com.equo.chromium.swt.OpenWindowListener;
import com.equo.chromium.swt.WindowEvent;
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
//	private static CefClientSwt popupClientHandler;
//	private static LifeSpanHandler popupLifeSpanHandler;
	private static CefMessageRouter router;
	
	Browser chromium;
	OpenWindowListener[] openWindowListeners = new OpenWindowListener[0];
	private CefFocusListener focusListener;
	private String url;
	private String title = "";
	private CompletableFuture<Boolean> enableProgress = new CompletableFuture<>();
	private CompletableFuture<Boolean> created = new CompletableFuture<>();
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
	private Stack<FunctionsResourceHandler> functionsResourceHandler = new Stack<FunctionsResourceHandler>();
	private HashMap<String, Boolean> changedCache = new HashMap<String, Boolean>();
	private CompletableFuture<Void> urls = CompletableFuture.completedFuture(null);
	private boolean isOpenDialog = false;
	private ControlAdapter fullscreenListener = null;

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
		created.thenRun(() -> {
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
					Object payload = Eval.decodeType(encoded, SWT.ERROR_INVALID_ARGUMENT);
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
								Display.getDefault().asyncExec(() -> {
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
		chromium.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
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

		cefBrowser = (CefBrowserSwt) clientHandler.createBrowser(this.url == null ? "about:blank" : this.url, false, false, null);
		cefBrowser.createImmediately(chromium);
	}

//	private void createPopup(long /*int*/ windowInfo, long /*int*/ client, WindowEvent event) {
//		if (paintListener != null) {
//			if (!Boolean.getBoolean("chromium.headless"))
//				chromium.removePaintListener(paintListener);
//			paintListener = null;
//		} else {
//			debug("Unregistering chromium phantom popup " + instance);
//			instances.remove(instance);
//		}
//		debug("Registering chromium popup new");
//		instancesNew.add(this);
//		isPopup = event;
//		
//		String platform = SWT.getPlatform ();
//		if ("gtk".equals(platform) && chromium.getDisplay().getActiveShell() != chromium.getShell()) {
//			// on linux the window hosting the popup needs to be created
//			boolean visible = chromium.getShell().isVisible();
//			chromium.getShell().open();
//			chromium.getShell().setVisible(visible);
//		}
//
//		prepareBrowser();
//		long /*int*/ popupHandle = hwnd;
//		debugPrint("popup will use hwnd:"+popupHandle);
//		Point size = new Point(0, 0);
//		if ("gtk".equals(SWT.getPlatform())) {
//			size = chromium.getParent().getSize();
//			// size = DPIUtil.autoScaleUp(size);
//		}
//
////		ChromiumLib.cefswt_set_window_info_parent(windowInfo, client, clientHandler.ptr, popupHandle, 0, 0, size.x, size.y);
//		debugPrint("reparent popup");
//	}
//	
//	private void createDefaultPopup(long /*int*/ windowInfo, long /*int*/ client, WindowEvent event) {
//		debugPrint("default popup");
//		debug("Registering chromium default popup new");
//
//		if (popupHandlers == 0) {
//			popupLifeSpanHandler = CEFFactory.newLifeSpanHandler();
//			popupLifeSpanHandler.on_after_created_cb = new CallbackExt(Chromium.class, "popup_on_after_created", void.class, new Type[] {long.class, long.class});
//			popupLifeSpanHandler.on_after_created = checkGetAddress(popupLifeSpanHandler.on_after_created_cb);
//			popupLifeSpanHandler.on_before_close_cb = new CallbackExt(Chromium.class, "popup_on_before_close", void.class, new Type[] {long.class, long.class});
//			popupLifeSpanHandler.on_before_close = checkGetAddress(popupLifeSpanHandler.on_before_close_cb);
//			popupLifeSpanHandler.do_close_cb = new CallbackExt(Chromium.class, "popup_do_close", int.class, new Type[] {long.class, long.class});
//			popupLifeSpanHandler.do_close = checkGetAddress(popupLifeSpanHandler.do_close_cb);
//			
//			popupLifeSpanHandler.ptr = C.malloc (cef_life_span_handler_t.sizeof);
//			ChromiumLib.memmove(popupLifeSpanHandler.ptr, popupLifeSpanHandler, cef_life_span_handler_t.sizeof);
//
//			if (popupClientHandler == null) {
//				popupClientHandler = CEFFactory.newClient();
//				popupClientHandler.get_life_span_handler_cb = new CallbackExt(Chromium.class, "popup_get_life_span_handler", long.class, new Type[] {long.class});
//				popupClientHandler.get_life_span_handler = checkGetAddress(popupClientHandler.get_life_span_handler_cb);
//			
//				popupClientHandler.ptr = C.malloc(cef_client_t.sizeof);
//				ChromiumLib.memmove(popupClientHandler.ptr, popupClientHandler, cef_client_t.sizeof);
//			}
//		}
//		popupHandlers++;
//
//		ChromiumLib.cefswt_set_window_info_parent(windowInfo, client, popupClientHandler.ptr, 0, event.location != null ? event.location.x : 0, event.location != null ? event.location.y : 0, event.size != null ? event.size.x : 0, event.size != null ? event.size.y : 0);
//	}
 
//	static long /*int*/ popup_get_life_span_handler(long /*int*/ client) {
//		if (popupLifeSpanHandler == null)
//			return 0;
//		//ChromiumLib.cefswt_ref(popupClientHandler.ptr, 1);
//		return popupLifeSpanHandler.ptr;
//	}
//	
//	static void popup_on_after_created(long /*int*/ plifeSpanHandler, long /*int*/ browser) {
//		int id = ChromiumLib.cefswt_get_id(browser);
//		debug("popup on_after_created: " + id);
//		try {
//			// not sleeping here causes deadlock with multiple window.open
//			Thread.sleep(LOOP);
//		} catch (InterruptedException e) {
//		}
//		ChromiumLib.cefswt_ref(browser, 0);
//	}
//	
//	static void popup_on_before_close(long /*int*/ plifeSpanHandler, long /*int*/ browser) {
//		debug("popup OnBeforeClose");
//		popupHandlers--;
//		if (popupHandlers == 0) {
//			disposeCallback(popupLifeSpanHandler.on_after_created_cb);
//			disposeCallback(popupLifeSpanHandler.do_close_cb);
//			disposeCallback(popupLifeSpanHandler.on_before_close_cb);
//			C.free(popupLifeSpanHandler.ptr);
//			popupLifeSpanHandler = null;
//		}
//		disposingAny--;
//		ChromiumLib.cefswt_ref(browser, 0);
//	}
//
//	static int popup_do_close(long /*int*/ plifeSpanHandler, long /*int*/ browser) {
//		debug("popup DoClose");
//		disposingAny++;
//		ChromiumLib.cefswt_ref(browser, 0);
//		return 0;
//	}
	
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
		
		router = Eval.createRouter();
		clientHandler.addMessageRouter(router);
	}
	
	static Chromium getChromium(CefBrowser cefBrowser) {
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
		fireCloseListener();
		for (BrowserFunction function : new ArrayList<>(functions.values())) {
			function.dispose(true);
		}
		functions.clear();
		if (disposing == Dispose.FromBrowser) {
			chromium.dispose();
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
				chromium.getDisplay().asyncExec(chromium::dispose);
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
		cefBrowser.resize();
		created.complete(true);
		if (this.url != null) {
			enableProgress.complete(true);
		}

		if (isPopup != null) {
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

		for (OpenWindowListener listener : openWindowListeners) {
			listener.open(event);
		}

		if (event.browser != null) {
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
				chromium.getDisplay().asyncExec(() -> {
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
				Vector<String> acceptFilters, int selectedAcceptFilter, CefFileDialogCallback callback) {
			// debug("onFileDialog", browser);
			if (!browser.isPopup()) {
				return getChromium(browser).onFileDialog(mode, title, defaultFilePath, acceptFilters, selectedAcceptFilter,
					callback);
			}
			return false;
		}
	}

	private boolean onFileDialog(CefDialogHandler.FileDialogMode mode, String title, String defaultFilePath,
			Vector<String> acceptFilters, int selectedAcceptFilter, CefFileDialogCallback callback) {
		if ("gtk".equals(SWT.getPlatform())) {
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

			FileDialog dlg = new FileDialog(chromium.getShell(), swtMode);
			dlg.setText(dlgTitle);
			String[] acceptFiltersArray = MimeTypeLinux.getExtensions(acceptFilters);
			dlg.setFilterExtensions(acceptFiltersArray);
			dlg.setOverwrite(true);

			String selected = dlg.open();
			if (selected != null) {
				Vector<String> filePaths = new Vector<String>();
				for (String fileName : dlg.getFileNames()) {
					filePaths.add(Paths.get(dlg.getFilterPath(), fileName).toString());
				}
				int filterIndex = dlg.getFilterIndex();
				filterIndex = (filterIndex < 0 || filterIndex >= acceptFiltersArray.length) ? selectedAcceptFilter : filterIndex;
				callback.Continue(filterIndex, filePaths);
			} else {
				callback.Cancel();
			}
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
		for (TitleListener listener : titleListeners) {
			listener.changed(event);
		}
	}

	private void onAddressChange(CefFrame frame, String url) {
		if (isDisposed()) return;

		if (Boolean.getBoolean("chromium.turbolinks") && !"about:blank".equals(url)) {
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
		chromium.getDisplay().asyncExec(() -> {
			for (LocationListener listener : locationListeners) {
				listener.changed(event);
			}
		});
	}

	private void onStatusMessage(String status) {
		if (isDisposed()) return;
		StatusTextEvent event = new StatusTextEvent(chromium);
		event.display = chromium.getDisplay ();
		event.widget = chromium;
		event.text = status;
		for (StatusTextListener listener : statusTextListeners) {
			listener.changed(event);
		}
	}

	private boolean onConsoleMessage(CefSettings.LogSeverity level, String message, String source, int line) {
		if (isDisposed()) return false;
		boolean prevent = false;
		if (extraApi != null) {
			for (ConsoleListener listener : extraApi.consoleListeners) {
				if (listener.message(level.ordinal(), message, source, line))
					prevent = true;
			}
		}
		return prevent;
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
			for (ProgressListener listener : progressListeners) {
				listener.changed(event);
			}
		}
	}

	private void onFullscreenModeChange(CefBrowser browser, boolean fullScreen) {
		if (isDisposed()) return;

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
	}

	private static void setRequestHandler() {
		requestHandler = new RequestHandler();
		clientHandler.addRequestHandler(requestHandler);
	}
	
	static class RequestHandler extends CefRequestHandlerAdapter {
		private static final String FUNCTION_HOST = "function"+new Random().nextInt();

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
			if (!browser.isPopup()) {
				getChromium(browser).getAuthCredentials(origin_url, host, port, realm, scheme, callback);
				return true;
			}
			return false;
		}
		
		@Override
		public boolean onCertificateError(CefBrowser browser, ErrorCode cert_error, String request_url,
				CefRequestCallback callback) {
			debug("onCertificateError", browser);
			if (!browser.isPopup()) {
				return getChromium(browser).onCertificateError(cert_error, request_url, callback);
			}
			return false;
		}
		
		@Override
		public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame,
				CefRequest request, boolean isNavigation, boolean isDownload, String requestInitiator,
				BoolRef disableDefaultHandling) {
			if (isFunction(browser, request)) {
				return new CefResourceRequestHandlerAdapter() {
					@Override
					public CefResourceHandler getResourceHandler(CefBrowser browser, CefFrame frame,
							CefRequest request) {
						if (!browser.isPopup()) {
							if (isPartial(request))
								return getChromium(browser).functionsResourceHandler.peek();
							return getChromium(browser).createFunctionResourceHandler();
						}
						return null;
					}
					
					@Override
					public void onProtocolExecution(CefBrowser browser, CefFrame frame, CefRequest request,
							BoolRef allowOsExecution) {
						debug("onProtocolExecution: "+request.getURL(), browser);
						allowOsExecution.set(true);
					}
				};
			} else {
				String requestUrl = request.getURL();
				if (!requestUrl.startsWith("http") && !requestUrl.startsWith("data") && !requestUrl.startsWith("file")
						&& !requestUrl.startsWith("chrome") && !requestUrl.startsWith("about")
						&& shouldAllowProtocol(requestUrl, browser)) {
					return new CefResourceRequestHandlerAdapter() {
						@Override
						public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
							return true;
						}
						@Override
						public void onProtocolExecution(CefBrowser browser, CefFrame frame, CefRequest request,
								BoolRef allowOsExecution) {
							debug("onProtocolExecution: " + request.getURL(), browser);
							browser.stopLoad();
							Program.launch(requestUrl);
						}
					};
				}
			}

			boolean isTurbolinkRequest = "empty".equals(request.getHeaderByName("sec-fetch-dest")) && !"navigate".equals(request.getHeaderByName("Sec-Fetch-Mode"))
					&& !"websocket".equals(request.getHeaderByName("Sec-Fetch-Mode"));

			if (Boolean.getBoolean("chromium.turbolinks") && isTurbolinkRequest) {
				return new CefResourceRequestHandlerAdapter() {
					@Override
					public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
						return true;
					}
				};
			}
			return null;
		}

		static private boolean isFunction(CefBrowser browser, CefRequest request) {
			if (!"POST".equals(request.getMethod()) || !request.getURL().contains(FUNCTION_HOST))
				return false;
			try {
				int functionIndex = getFunctionIndex(request);
				
				return getChromium(browser).functions.containsKey(functionIndex);
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
				Display.getDefault().syncExec(() -> {
					boolean isPopup = browser.isPopup();
					Chromium chromium = isPopup ? getChromium(((CefPopup)browser).getParent()) : getChromium(browser);
					chromium.openJsDialog(isPopup, CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_CONFIRM, "",
							"The link will be opened with the operating system.", "Do you want to continue?",
							"default_prompt_text", callback);
				});

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
		LocationEvent event = new LocationEvent(chromium);
		event.display = chromium.getDisplay();
		event.widget = chromium;
		event.doit = true;
		event.top = frame.isMain();
		event.location = getPlainUrl(request.getURL());
		debugPrint("on_before_browse:" + event.location + " top:"+event.top);
		try {
//			loopDisable = true;
			for (LocationListener listener : locationListeners) {
				listener.changing(event);
			}
		} finally {
//			loopDisable = false;
		}

		if(Boolean.getBoolean("chromium.turbolinks")) {
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
		chromium.getDisplay().syncExec(() -> {
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

	private boolean onCertificateError(ErrorCode cert_error, String request_url, CefRequestCallback callback) {
		String derpem = System.getProperty("chromium.ssl", "");
		if (!derpem.isEmpty()) {
			if (Files.isReadable(Paths.get(derpem))) {
				try {
					byte[] derpemBytes = Files.readAllBytes(Paths.get(derpem));
					callback.Continue(true);
					return true;
				} catch (IOException e) {
					debugPrint("Failed to read file "+derpem);
					e.printStackTrace();
				}
			} else {
				debugPrint("Cannot read file '"+derpem+"', trying as string");
				byte[] derpemBytes = derpem.getBytes(Charset.forName("ASCII"));
				callback.Continue(true);
				return true;
			}
		}
		
		String javaHost = request_url;
		MessageBox prompt = new MessageBox (chromium.getShell(), SWT.YES | SWT.NO);
		prompt.setText(SWT.getMessage("SWT_InvalidCert_Title"));
		String cefError = cert_error.toString();
		String specific = cefError.isEmpty() ? "\n\n" : "\n\n" + cefError + "\n\n";
		String message = SWT.getMessage("SWT_InvalidCert_Message", new Object[] {javaHost}) +
				specific + SWT.getMessage("SWT_InvalidCert_Connect");
		prompt.setMessage(message);
		int result = prompt.open();
		if (result == SWT.YES) {
			callback.Continue(true);
		} else {
			callback.Cancel();
		}
		return true;
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
			if (!useSwtDialogs()) {
				return false;
			}
			if (!browser.isPopup()) {
				return getChromium(browser).onJsdialog(browser, origin_url, dialog_type, message_text, default_prompt_text, callback, suppress_message);
			}
			return false;
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
		if (isDisposed()) return false;
		
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
		openJsDialog(false, dialog_type, title, message_text, prompt, default_prompt_text, callback);
		return true;
	}

	public void openJsDialog(boolean isPopup, JSDialogType dialog_type, String title, String msg, String prompt,
			String default_prompt_text, CefJSDialogCallback callback) {
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
		Consumer<Integer> close = open -> {
			boolean r = open == SWT.OK || open == SWT.YES ? true : false;
			debug("JS Dialog Closed with "+r);
			if(browser != null && !isPopup) {
				if (disposing == Dispose.Unload && !r)
					disposing = Dispose.UnloadCancel;
				chromium.getDisplay().asyncExec(() -> {
					callback.Continue(r, default_prompt_text);
				});
				chromium.getShell().forceActive();
			} else {
				Display.getDefault().asyncExec(() -> {
					callback.Continue(r, default_prompt_text);
				});
			}
		};
		if (!"test".equals(System.getProperty("chromium.dialogs", "")) || browser == null) {
			Shell shell = (browser != null && !isPopup) ?  chromium.getShell() : new Shell();
			MessageBox box = new MessageBox(shell, style);
			box.setText(title);
			box.setMessage(prompt != null ? msg + "\n\n"+prompt : msg);
			isOpenDialog = true;
			int open = box.open();
			isOpenDialog = false;
			close.accept(open);
		} else {
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
//			Display.getDefault().asyncExec(() -> {
//				ChromiumLib.cefswt_dialog_close(callback, 0, 0);
				openJsDialog(false, JSDialogType.JSDIALOGTYPE_CONFIRM, "Are you sure you want to leave this page?", message_text, null, null, callback);
				if (disposing == Dispose.Unload) {
					disposing = Dispose.UnloadClosed;
				}
//			});
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
		if (chromium.getMenu() != null) {
			model.clear();
			chromium.getMenu().setVisible(true);
		}
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
			if (!browser.isPopup()) {
				getChromium(browser).onBeforeDownload(downloadItem, suggestedName, callback);
			}
		}

		@Override
		public void onDownloadUpdated(CefBrowser browser, CefDownloadItem downloadItem,
				CefDownloadItemCallback callback) {
			debug("onDownloadUpdated", browser);
			if (!browser.isPopup()) {
				getChromium(browser).onDownloadUpdated(downloadItem, callback);
			}
		}
	}
	
	private void onBeforeDownload(CefDownloadItem download_item, String suggested_name, CefBeforeDownloadCallback callback) {
		String name = suggested_name;
		if ("gtk".equals(SWT.getPlatform())) {
			FileDialog dlg = new FileDialog(chromium.getShell(), SWT.SAVE);
			dlg.setText("Save File");
			dlg.setOverwrite(true);
			if (name != null && !name.isEmpty()) {
				dlg.setFileName(name);
			}
			String selected = dlg.open();
			if (selected != null) {
				if (download_item.isValid()) {
					new Download(download_item, selected, this);
				}
				callback.Continue(selected, false);
			}
		} else {
			if (download_item.isValid()) {
				new Download(download_item, name, this);
			}
			callback.Continue("", true);
		}
	}

	private void onDownloadUpdated(CefDownloadItem download_item, CefDownloadItemCallback callback) {
		if (download_item.isValid()) {
			Download download = Download.downloads.get(download_item.getId());
			if (download != null) {
				download.update(download_item, callback);
				if (download_item.isCanceled()) download.dispose();
				else if (!download.open && !Boolean.getBoolean("chromium.disable-download-progress")) download.show();
			}
		}
	}

	static class Download {
		static final Map<Integer, Download> downloads = new HashMap<>();
		private int id;
		private Shell shell;
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
		
		public Download(CefDownloadItem download_item, String name, Chromium browser) {
			this.id = download_item.getId();
			Download.downloads.put(download_item.getId(), this);
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
			Monitor monitor = browser.chromium.getMonitor ();
			int maxWidth = monitor.getBounds ().width / 2;
			int width = nameLabel.computeSize (SWT.DEFAULT, SWT.DEFAULT).x;
			data.widthHint = Math.min (width, maxWidth);
			data.horizontalAlignment = GridData.FILL;
			data.grabExcessHorizontalSpace = true;
			nameLabel.setLayoutData (data);
			
			int pbStyle = download_item.getPercentComplete() == -1 ? SWT.SMOOTH | SWT.HORIZONTAL | SWT.INDETERMINATE : SWT.SMOOTH | SWT.HORIZONTAL;
			pb = new ProgressBar(shell, pbStyle);
			pb.setSelection(download_item.getPercentComplete());
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
			this.cancel_cb = callback;
			if (shell.isDisposed()) {
				dispose();
				return;
			}
			if (download_item.isComplete()) {
				Display.getDefault().timerExec(3000, this::dispose);
			}
			
			setText(download_item);
			
			long current = download_item.getReceivedBytes() / 1024L;
			long total = download_item.getTotalBytes() / 1024L;
			String message = Compatibility.getMessage ("SWT_Download_Status", new Object[] {Long.valueOf(current), new Long(total)}); //$NON-NLS-1$
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

		private void setText(CefDownloadItem download_item) {
			String nameString = download_item.getFullPath();
			String urlString = download_item.getURL();
			String msg = Compatibility.getMessage ("SWT_Download_Location", new Object[] {nameString +"\n", urlString}); //$NON-NLS-1$
			nameLabel.setText (msg);
		}

		private void dispose() {
			Download.downloads.remove(id);
			cancel_cb = null;
			
			shell.dispose();
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
		
		chromium.getDisplay().asyncExec(() -> {
			if (!isDisposed()) {
				sendKeyEvent(firedEvent);
			}
		});
		
		if (firedEvent.doit && "cocoa".equals(SWT.getPlatform())) {
			if (event.modifiers == 128 && event.type != EventType.KEYEVENT_RAWKEYDOWN) {
				if (event.native_key_code >= 8 && event.native_key_code <= 9) {
					return true;
				}
			} else if (event.modifiers == 128 && event.type == EventType.KEYEVENT_RAWKEYDOWN) {
				switch (event.native_key_code) {
				// Select all: command + a
				case 0:
					cefBrowser.getMainFrame().selectAll();
					return true;
				// Copy: command + c
				case 8:
					cefBrowser.getMainFrame().copy();
					return true;
				// Paste: command + v
				case 9:
					cefBrowser.getMainFrame().paste();
					CefApp.getInstance().doMessageLoopWork(-1);
					return true;
				// Cut: command + x
				case 7:
					cefBrowser.getMainFrame().cut();
					return true;
				// Undo: command + z
				case 6:
					cefBrowser.getMainFrame().undo();
					CefApp.getInstance().doMessageLoopWork(-1);
					return true;
				default:
					return false;
				}
			} else if (event.modifiers == 130 && event.type == EventType.KEYEVENT_RAWKEYDOWN) {
				// Redo: command + shift + z
				if (event.native_key_code == 6) {
					cefBrowser.getMainFrame().redo();
					CefApp.getInstance().doMessageLoopWork(-1);
					return true;
				}
			}
		}
		//Exit fullSreen
		if (fullscreenListener != null && event.windows_key_code == 27
				&& event.type == EventType.KEYEVENT_RAWKEYDOWN) {
			cefBrowser.getComposite().getShell().setFullScreen(false);
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
		if (!isDisposed()) {
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
		}
	}

	private boolean onSetFocus(FocusSource focusSource) {
		if (isDisposed()) return true;
		boolean chromiumFocusControl = "gtk".equals(SWT.getPlatform()) ? true : chromium.getDisplay().getFocusControl() != chromium;
		if (ignoreFirstFocus && chromiumFocusControl && focusSource == FocusSource.FOCUS_SOURCE_NAVIGATION) {
			debugPrint("ignoreFirstFocus "+chromium.getDisplay().getFocusControl());
			ignoreFirstFocus = false;
			return true;
		}
		if ("win32".equals(SWT.getPlatform())) { // consider this for all os
			if (focusListener != null)
				focusListener.enabled = false;
			chromium.setFocus();
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
	}

	private static void setupCookies() {
		WebBrowser.NativeClearSessions = () -> {
			Engine.ready.thenRun(() -> {
				CefCookieManager globalManager = CefCookieManager.getGlobalManager();
				if (globalManager != null) {
					globalManager.deleteCookies(null, null);
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
				CefCookie cefCookie = new CefCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getSecure(), cookie.isHttpOnly(), now, now, cookie.getMaxAge() != -1, expires);
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
					debug("Visitor " + count + "/" +total + ": " + cookie.name+ ":" + Thread.currentThread());
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
		
		try {
			if (!functionsResourceHandler.isEmpty() && functionsResourceHandler.peek().inFunction) {
				return Eval.eval(script, getPlainUrl(url), cefBrowser, router, created, functionsResourceHandler.peek());
			}
			return Eval.eval(script, getPlainUrl(url), cefBrowser, router, created);
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
			cefBrowser.stopLoad();
		}
	}
	
	boolean isDisposed() {
		return chromium == null || chromium.isDisposed();
	}
	
	class ExtraApi implements ChromiumBrowser {
		String lastSearch = null;
		List<ConsoleListener> consoleListeners = new ArrayList<ConsoleListener>();
		List<Object> errors;

		@Override
		public void find(String search, boolean forward, boolean matchCase) {
			checkBrowser();
			boolean findNext = Objects.equals(search, lastSearch);
			lastSearch = search;
			cefBrowser.find(1, search, forward, matchCase, findNext);
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
			cefBrowser.setZoomLevel(zoomLevel);
		}

		@Override
		public void addConsoleListener(ConsoleListener listener) {
			consoleListeners.add(listener);
		}

		@Override
		public void removeConsoleListener(ConsoleListener listener) {
			consoleListeners.remove(listener);
		}
		
	}
}
