package org.eclipse.swt.chromium;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.HttpCookie;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.C;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.internal.Library;
import org.eclipse.swt.internal.chromium.CEFFactory;
import org.eclipse.swt.internal.chromium.CEFFactory.EvalReturned;
import org.eclipse.swt.internal.chromium.CEFFactory.ReturnType;
import org.eclipse.swt.internal.chromium.ChromiumLib;
import org.eclipse.swt.internal.chromium.FunctionSt;
import org.eclipse.swt.internal.chromium.ResourceExpander;
import org.eclipse.swt.internal.chromium.cef_app_t;
import org.eclipse.swt.internal.chromium.cef_browser_process_handler_t;
import org.eclipse.swt.internal.chromium.cef_client_t;
import org.eclipse.swt.internal.chromium.cef_context_menu_handler_t;
import org.eclipse.swt.internal.chromium.cef_cookie_visitor_t;
import org.eclipse.swt.internal.chromium.cef_display_handler_t;
import org.eclipse.swt.internal.chromium.cef_focus_handler_t;
import org.eclipse.swt.internal.chromium.cef_jsdialog_handler_t;
import org.eclipse.swt.internal.chromium.cef_life_span_handler_t;
import org.eclipse.swt.internal.chromium.cef_load_handler_t;
import org.eclipse.swt.internal.chromium.cef_popup_features_t;
import org.eclipse.swt.internal.chromium.cef_request_handler_t;
import org.eclipse.swt.internal.chromium.cef_string_visitor_t;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Widget;

class Chromium extends WebBrowser {
	private static final String DATA_TEXT_URL = "data:text/html;base64,";
	private static final String VERSION = "0901";
    private static final String CEFVERSION = "3071";
    private static final String SHARED_LIB_V = "chromium_swt_"+VERSION;
    private static final String JNI_LIB_V = "swt-chromium-"+VERSION;
    private static final int MAX_PROGRESS = 100;
    private static final int LOOP = 50;
    
    Browser chromium;
    OpenWindowListener[] openWindowListeners = new OpenWindowListener[0];

    static {
        lib = loadLib();
    }
    
    private static Object lib;
    private static String cefrustPath;
    private static AtomicInteger browsers = new AtomicInteger(0);
//    private static CompletableFuture<Boolean> cefInitilized;
    private static cef_app_t app;
    private static cef_browser_process_handler_t browserProcessHandler;
    private static boolean shuttindDown;
    private static cef_cookie_visitor_t cookieVisitor;
    private static CompletableFuture<Boolean> cookieVisited;
    private static int EVAL = 1;
    private static int INSTANCES = 0;
    private static Runnable loopWork;
    private static boolean loopDisable;
    private static boolean pumpDisable;
    private static int disposingAny = 0;

    private long hwnd;
    private long browser;
    private cef_client_t clientHandler;
    private cef_focus_handler_t focusHandler;
    private cef_life_span_handler_t lifeSpanHandler;
    private cef_load_handler_t loadHandler;
    private cef_display_handler_t displayHandler;
    private cef_request_handler_t requestHandler;
    private cef_jsdialog_handler_t jsDialogHandler;
    private cef_context_menu_handler_t contextMenuHandler;
    private cef_string_visitor_t  textVisitor;
    private FocusListener focusListener;
    private String url;
    private String postData;
    private String[] headers;
    private String text = "";
    private CompletableFuture<String> textReady;
    private CompletableFuture<Boolean> loadComplete;
    private boolean canGoBack;
    private boolean canGoForward;
    private CompletableFuture<Boolean> enableProgress = new CompletableFuture<>();
    private CompletableFuture<Boolean> created = new CompletableFuture<>();
    private int disposing;
    private int instance;
	private boolean hasFocus;
	private boolean ignoreFirstFocus = true;
	private PaintListener paintListener;
	private WindowEvent isPopup;
    private List<DefaultPopup> popupHandlers = new ArrayList<>();

    public Chromium() {
        instance = ++INSTANCES;
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
        this.chromium = browser;
    }
    
    @Override
    public void createFunction (BrowserFunction function) {
    	created.thenRun(() -> {
    		checkBrowser();
    		
    		for (BrowserFunction current : functions.values()) {
    			if (current.name.equals (function.name)) {
    				deregisterFunction (current);
    				break;
    			}
    		}
    		function.index = getNextFunctionIndex();
    		registerFunction(function);
    		
    		if (!ChromiumLib.cefswt_function(browser, function.name, function.index)) {
    			throw new SWTException("Cannot create BrowserFunction");
    		}
    	});
    }

    public void destroyFunction (BrowserFunction function) {
        checkBrowser();
    }
    
    @Override
    public void create(Composite parent, int style) {
        initCEF(chromium.getDisplay());
//        debugPrint("initCef Done");
        paintListener = new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
            	debugPrint("paintControl");
            	chromium.removePaintListener(this);
                createBrowser();
                paintListener = null;
//                cefInitilized.thenRun(() -> { 
//                    debugPrint("cefInitilized Future CALLBACK");
//                    chromium.getDisplay().syncExec(() -> {
//                        try {
//                        } catch(Throwable e1) {
//                            browserInitilized.completeExceptionally(e1);
//                        }
//                    });
//                });
//                debugPrint("paintControl Done");
            }
        };
		chromium.addPaintListener(paintListener);
    }

    private Object debugPrint(String log) {
        System.out.println("J"+instance + ":" + Thread.currentThread().getName() +":" + log + (this.url != null ? " (" + getPlainUrl(this.url) + ")" : " empty-url"));
        return null;
    }
    
    private static Object debug(String log) {
        System.out.println("J:" + log);
        return null;
    }

    private void initCEF(Display display) {
        synchronized (lib) {
            if (app == null) {
//                CEFFactory.create();
                app = CEFFactory.newApp();
//                cefInitilized = new CompletableFuture<>();
                browserProcessHandler = CEFFactory.newBrowserProcessHandler();
//                browserProcessHandler.on_context_initialized_cb = new Callback(this, "on_context_initialized", void.class, new Type[] {long.class});
//                browserProcessHandler.on_context_initialized = browserProcessHandler.on_context_initialized_cb.getAddress();
                browserProcessHandler.on_schedule_message_pump_work_cb = new Callback(Chromium.class, "on_schedule_message_pump_work", void.class, new Type[] {long.class, int.class});
                browserProcessHandler.on_schedule_message_pump_work = checkGetAddress(browserProcessHandler.on_schedule_message_pump_work_cb);
//              
                app.get_browser_process_handler_cb = new Callback(Chromium.class, "get_browser_process_handler", long.class, new Type[] {long.class});
                app.get_browser_process_handler = checkGetAddress(app.get_browser_process_handler_cb);
              
                browserProcessHandler.ptr = C.malloc (cef_browser_process_handler_t.sizeof);
                ChromiumLib.memmove(browserProcessHandler.ptr, browserProcessHandler, cef_browser_process_handler_t.sizeof);

                System.out.println("cefrust.path: " + cefrustPath);
                int debugPort = 0;
                try {
                	debugPort = Integer.parseInt(System.getProperty("org.eclipse.swt.chromium.remote-debugging-port", "0"));
                } catch (NumberFormatException e) {
                	debugPort = 0;
                }
                app.ptr = C.malloc(cef_app_t.sizeof);
                ChromiumLib.memmove(app.ptr, app, cef_app_t.sizeof);
                ChromiumLib.cefswt_init(app.ptr, cefrustPath, VERSION, debugPort);
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public synchronized void start() {
                if (app == null || shuttindDown) {
                    // already shutdown
                    return;
                }
                if (Display.getCurrent() != null) {
                    internalShutdown();
                } else {
                    Display.getDefault().syncExec(() -> internalShutdown());
                }
            }
        });
    }

    static long get_browser_process_handler(long app) {
//        debug("GetBrowserProcessHandler");
    	if (browserProcessHandler == null)
    		return 0;
        return browserProcessHandler.ptr;
    }

//    void on_context_initialized(long browserProcessHandler) {
//    	ChromiumLib.lock.lock();
//    	try {
//        debugPrint("OnContextInitialized");
//        cefInitilized.complete(true);
//    	} finally {
//            ChromiumLib.lock.unlock();
//        }
//    }

    static Runnable loopWorkRunnable = () -> {
        Display display = Display.getCurrent();
    	if (display == null || display.isDisposed() /*|| display.getActiveShell() != getShell()*/) {
            //System.err.println("Ignore do_message_loop_work due inactive shell");
            return;
        }
//        debug("WORK PUMP");
        safe_loop_work();
    };
    
    static void on_schedule_message_pump_work(long pbrowserProcessHandler, int delay) {
        if (browsers.get() <= 0 || pumpDisable || disposingAny > 0)
            return;
        Display display = Display.getDefault();
//        debugPrint("pump "+delay);
        Runnable scheduleWork = () -> {
            restartLoop(display, delay);
            display.timerExec(-1, loopWorkRunnable);
//                  debug("WORK PUMP DELAYED");
            display.timerExec(delay, loopWorkRunnable);
        };
        if (Display.getCurrent() != null) {
            if (delay <= 0) {
                restartLoop(display, 0);
//              debug("WORK PUMP NOW");
                display.asyncExec(loopWorkRunnable);
            } else {
                scheduleWork.run();
            }
        } else {
            if (delay <= 0) {
                display.asyncExec(() -> {
                    restartLoop(display, 0);
//                    debug("WORK PUMP ALMOST NOW");
                    loopWorkRunnable.run();
                });
            } else {
                display.asyncExec(scheduleWork);
            }
        }
    }

    private static void safe_loop_work() {
        if (browsers.get() > 0 && !loopDisable) {
        	if (ChromiumLib.cefswt_do_message_loop_work() == 0) {
        	    System.err.println("error looping");
        	}
        	if (pumpDisable == true) {
        	    pumpDisable = false;
        	}
        }
    }

	private static void restartLoop(Display display, int ms) {
		if (loopWork != null) {
			display.timerExec(-1, loopWork);
			display.timerExec(LOOP + ms, loopWork);
		}
	}
    
    private long getHandle(Composite control) {
        long hwnd = 0;
        String platform = SWT.getPlatform ();
        if ("cocoa".equals(platform)) {
            try {
                Field field = Control.class.getDeclaredField("view");
                field.setAccessible(true);
                Object nsview = field.get(control);
                
                Class<?> idClass = Class.forName("org.eclipse.swt.internal.cocoa.id");
                Field idField = idClass.getField("id");

                hwnd = idField.getLong(nsview);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("win32".equals(platform)) {
            try {
                Field field = Control.class.getDeclaredField("handle");
                field.setAccessible(true);
                hwnd = (long) field.get(control);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                Field field = Widget.class.getDeclaredField("handle");
                field.setAccessible(true);
                hwnd = (long) field.get(control);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return hwnd;
    }
    
	private void prepareBrowser() {
		hwnd = getHandle(chromium);

        chromium.addDisposeListener(e -> {
            debugPrint("disposing chromium");
            dispose();
        });
        focusListener = new CefFocusListener();
        chromium.addFocusListener(focusListener);

        set_text_visitor();
        clientHandler = CEFFactory.newClient();
        set_focus_handler();
        set_life_span_handler();
        set_load_handler();
        set_display_handler();
        set_request_handler();
        set_jsdialog_handler();
        set_context_menu_handler();
        clientHandler.on_process_message_received_cb = new Callback(this, "on_process_message_received", int.class, new Type[] {long.class, long.class, int.class, long.class});
        clientHandler.on_process_message_received = checkGetAddress(clientHandler.on_process_message_received_cb);
        
        chromium.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                if (!isDisposed() && browser != 0) {
                    Point size = getChromiumSize();
					ChromiumLib.cefswt_resized(browser,  size.x,  size.y);
                }
            }
        });
        clientHandler.ptr = C.malloc(cef_client_t.sizeof);
        ChromiumLib.memmove(clientHandler.ptr, clientHandler, cef_client_t.sizeof);
	}

    private void createBrowser() {
        if (this.url == null) {
            this.url = "about:blank";
        }
        prepareBrowser();
        final Display display = chromium.getDisplay();
        final Color bgColor = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        int cefBgColor = cefColor(bgColor.getAlpha(), bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue());

        final org.eclipse.swt.graphics.Point size = getChromiumSize();
        ChromiumLib.cefswt_create_browser(hwnd, url, clientHandler.ptr, size.x, size.y, jsEnabledOnNextPage ? 1 : 0, cefBgColor);
    }

    private void createPopup(long windowInfo, long client, WindowEvent event) {
    	if (paintListener != null) {
    		chromium.removePaintListener(paintListener);
    		paintListener = null;
    	}
    	isPopup = event;
    	
        String platform = SWT.getPlatform ();
    	if ("gtk".equals(platform) && chromium.getDisplay().getActiveShell() != chromium.getShell()) {
    	    // on linux the window hosting the popup needs to be created
    	    boolean visible = chromium.getShell().isVisible();
    	    chromium.getShell().open();
    	    chromium.getShell().setVisible(visible);
    	}

        prepareBrowser();
    	long popupHandle =  hwnd;
    	debugPrint("popup will use hwnd:"+popupHandle);
        Point size = chromium.getParent().getSize();
        size = DPIUtil.autoScaleUp(size);

    	ChromiumLib.cefswt_set_window_info_parent(windowInfo, client, clientHandler.ptr, popupHandle, 0, 0, size.x, size.y);
    	debugPrint("reparent popup");
	}
    
    private void createDefaultPopup(long windowInfo, long client, WindowEvent event) {
        popupHandlers.add(new DefaultPopup(windowInfo, client, event));
        debugPrint("default popup");
    }
    
    class DefaultPopup {
        private cef_client_t nullHandler;
        private cef_life_span_handler_t popupHandler;

        public DefaultPopup(long windowInfo, long client, WindowEvent event) {
//            CEF.cef_client_t nullHandler = null;
            nullHandler = CEFFactory.newClient();
            popupHandler = CEFFactory.newLifeSpanHandler();
            popupHandler.on_after_created_cb = new Callback(this, "popup_on_after_created", void.class, new Type[] {long.class, long.class});
            popupHandler.on_after_created = checkGetAddress(popupHandler.on_after_created_cb);
            
            popupHandler.on_before_close_cb = new Callback(this, "popup_on_before_close",  void.class, new Type[] {long.class, long.class});
            popupHandler.on_before_close = checkGetAddress(popupHandler.on_before_close_cb);
            
            popupHandler.do_close_cb = new Callback(this, "popup_do_close", int.class, new Type[] {long.class, long.class});
            popupHandler.do_close = checkGetAddress(popupHandler.do_close_cb);
            
            nullHandler.get_life_span_handler_cb = new Callback(this, "popup_get_life_span_handler", long.class, new Type[] {long.class});
            nullHandler.get_life_span_handler = checkGetAddress(nullHandler.get_life_span_handler_cb);
            popupHandler.ptr = C.malloc (cef_life_span_handler_t.sizeof);
            ChromiumLib.memmove(popupHandler.ptr, popupHandler, cef_life_span_handler_t.sizeof);
            
            nullHandler.ptr = C.malloc(cef_client_t.sizeof);
            ChromiumLib.memmove(nullHandler.ptr, nullHandler, cef_client_t.sizeof);

            ChromiumLib.cefswt_set_window_info_parent(windowInfo, client, nullHandler.ptr, 0, event.location.x, event.location.y, event.size.x, event.size.y);
        }
        
        long popup_get_life_span_handler(long client) {
        	if (popupHandler == null)
        		return 0;
            return popupHandler.ptr;
        }
        
        void popup_on_after_created(long plifeSpanHandler, long browser) {
            debug("popup on_after_created");
            try {
                // not sleeping here causes deadlock with multiple window.open
                Thread.sleep(LOOP);
            } catch (InterruptedException e) {
            }
        }
        
        void popup_on_before_close(long plifeSpanHandler, long browser) {
            debug("popup OnBeforeClose");
            popupHandler.on_after_created_cb.dispose();
            popupHandler.do_close_cb.dispose();
            popupHandler.on_before_close_cb.dispose();
            C.free(popupHandler.ptr);
            popupHandler = null;
            
            nullHandler.get_life_span_handler_cb.dispose();
            C.free(nullHandler.ptr);
            nullHandler = null;
            popupHandlers.remove(this);
            disposingAny--;
        }
        
        int popup_do_close(long plifeSpanHandler, long browser) {
            debug("popup DoClose");
            disposingAny++;
            return 0;
        }
    }
    
    private int cefColor(int a, int r, int g, int b) {
    	return (a << 24) | (r << 16) | (g << 8) | (b << 0);
    }

    private Point getChromiumSize() {
    	Point size = chromium.getSize();
    	return DPIUtil.autoScaleUp(size);
    }

    private void set_life_span_handler() {
        lifeSpanHandler = CEFFactory.newLifeSpanHandler();
        lifeSpanHandler.on_before_close_cb = new Callback(this, "on_before_close", void.class, new Type[] {long.class, long.class});
        lifeSpanHandler.on_before_close = checkGetAddress(lifeSpanHandler.on_before_close_cb);
        lifeSpanHandler.do_close_cb = new Callback(this, "do_close", int.class, new Type[] {long.class, long.class});
        lifeSpanHandler.do_close = checkGetAddress(lifeSpanHandler.do_close_cb);
        lifeSpanHandler.on_after_created_cb = new Callback(this, "on_after_created", void.class, new Type[] {long.class, long.class});
        lifeSpanHandler.on_after_created = checkGetAddress(lifeSpanHandler.on_after_created_cb);
        lifeSpanHandler.on_before_popup_cb = new Callback(this, "on_before_popup", int.class, new Type[] {long.class, long.class, long.class,
                long.class, long.class, int.class,
                int.class, long.class, long.class,
                long.class, long.class, int.class});
        lifeSpanHandler.on_before_popup = checkGetAddress(lifeSpanHandler.on_before_popup_cb);
        
        clientHandler.get_life_span_handler_cb = new Callback(this, "get_life_span_handler", long.class, new Type[] {long.class});
        clientHandler.get_life_span_handler = checkGetAddress(clientHandler.get_life_span_handler_cb);
        lifeSpanHandler.ptr = C.malloc (cef_life_span_handler_t.sizeof);
        ChromiumLib.memmove(lifeSpanHandler.ptr, lifeSpanHandler, cef_life_span_handler_t.sizeof);
    }
    
    long get_life_span_handler(long client) {
        debugPrint("GetLifeSpanHandler");
        if (lifeSpanHandler == null)
    		return 0;
        return lifeSpanHandler.ptr;
    }

    void on_before_close(long plifeSpanHandler, long browser) {
        debugPrint("OnBeforeClose");
		Display display = Display.getCurrent();
		debugPrint("freelAll now");
		freeAll(display);
		ChromiumLib.cefswt_free(browser);
		this.browser = 0;
		this.chromium = null;
        debugPrint("closed");
        // not always called on linux
        disposingAny--;
        if (browsers.decrementAndGet() == 0 && shuttindDown) {
            internalShutdown();
        }
    }
    
    private synchronized void freeAll(Display display) {
        if (clientHandler != null) {
        	C.free(clientHandler.ptr);
	        disposeCallback(clientHandler.get_context_menu_handler_cb);
	        disposeCallback(clientHandler.get_display_handler_cb);
	        disposeCallback(clientHandler.get_focus_handler_cb);
	        if (jsDialogHandler != null) {
	        	disposeCallback(clientHandler.get_jsdialog_handler_cb);
	        }
	        
	        Callback get_life_span_handler_cb = clientHandler.get_life_span_handler_cb;
	        Callback get_request_handler_cb = clientHandler.get_request_handler_cb;
	        display.asyncExec(() -> {
				disposeCallback(get_life_span_handler_cb);
				disposeCallback(get_request_handler_cb);
	        });
	        disposeCallback(clientHandler.get_load_handler_cb);
        		
	        disposeCallback(clientHandler.on_process_message_received_cb);
	        clientHandler = null;
        }
        if (focusHandler != null) {
	        C.free(focusHandler.ptr);
	        disposeCallback(focusHandler.on_got_focus_cb);
	        disposeCallback(focusHandler.on_set_focus_cb);
	        disposeCallback(focusHandler.on_take_focus_cb);
	        focusHandler = null;
        }
        if (lifeSpanHandler != null) {
	        disposeCallback(lifeSpanHandler.do_close_cb);
	        disposeCallback(lifeSpanHandler.on_after_created_cb);
	        disposeCallback(lifeSpanHandler.on_before_close_cb);
	        disposeCallback(lifeSpanHandler.on_before_popup_cb);
	        C.free(lifeSpanHandler.ptr);
	        lifeSpanHandler = null;
        }
        if (loadHandler != null) {
	        disposeCallback(loadHandler.on_loading_state_change_cb);
	        C.free(loadHandler.ptr);
	        loadHandler = null;
        }
        if (displayHandler != null) {
		    disposeCallback(displayHandler.on_address_change_cb);
		    disposeCallback(displayHandler.on_status_message_cb);
		    disposeCallback(displayHandler.on_title_change_cb);
	        C.free(displayHandler.ptr);
		    displayHandler = null;
        }
        if (requestHandler != null) {
        	disposeCallback(requestHandler.on_before_browse_cb);
        	C.free(requestHandler.ptr);
        	requestHandler = null;
        }
        if (jsDialogHandler != null) {
        	disposeCallback(jsDialogHandler.on_jsdialog_cb);
	        C.free(jsDialogHandler.ptr);
	        jsDialogHandler = null;
        }
        if (contextMenuHandler != null) {
	        disposeCallback(contextMenuHandler.run_context_menu_cb);
	        C.free(contextMenuHandler.ptr);
	        contextMenuHandler = null;
        }
        
        if (textVisitor != null) {
        	if (textReady != null) {
        		textReady.thenRun(() -> {
        			disposeCallback(textVisitor.visit_cb);
        			C.free(textVisitor.ptr);
        			textVisitor = null;
        		});
        	} else {
        		disposeCallback(textVisitor.visit_cb);
    			C.free(textVisitor.ptr);
    			textVisitor = null;
        	}
        }
        
        debugPrint("ALL DISPOSED");
	}

	int do_close(long plifeSpanHandler, long browser) {
        //lifeSpanHandler.base.ref++;
        if (!ChromiumLib.cefswt_is_same(Chromium.this.browser, browser)) {
            debugPrint("DoClose popup:" + Chromium.this.browser+":"+browser);
            return 0;
        }
        debugPrint("DoClose");
        Display display = chromium.getDisplay();
		if (/*!disposing &&*/ !isDisposed() && closeWindowListeners != null) {
            org.eclipse.swt.browser.WindowEvent event = new org.eclipse.swt.browser.WindowEvent(chromium);
            event.display = display;
            event.widget = chromium;
//            event.browser = chromium;
            for (CloseWindowListener listener : closeWindowListeners) {
                listener.close(event);
            }
        }
        
        if (disposing == 0) {
        	if (chromium != null) {
        		disposing = 2;
        		chromium.dispose();
        	}
        }
//        if ("gtk".equals(SWT.getPlatform())) {
        	waitForClose(display);
//        }
        // do not send close notification to top level window
        // returning 0, cause the window to close 
        debugPrint("AFTER DoClose");

        return 1;
    }

    void on_after_created(long self, long browser) {
    	try {
//    		ChromiumLib.lock.lock();
        if (isDisposed() || visibilityWindowListeners == null) return;
        debugPrint("on_after_created " + browser);
        if (browser != 0) {
            Chromium.this.browser = browser;
            browsers.incrementAndGet();
            if (this.isPopup == null) {
                final org.eclipse.swt.graphics.Point size = getChromiumSize();
                ChromiumLib.cefswt_resized(browser, size.x,  size.y);
            }
            if (this.isPopup != null && this.url != null) {
                debugPrint("load url after created");
                doSetUrlPost(browser, url, postData, headers);
            }
            else if (!"about:blank".equals(this.url)) {
                enableProgress.complete(true);
            }
        }
        created.complete(true);

        if (browsers.get() == 1) {
            debugPrint("STARTING MSG LOOP");
            final Display display = chromium.getDisplay();
            doMessageLoop(display);
        }
        
//        chromium.getDisplay().asyncExec(() -> {
            debugPrint("on_after_created handling " + browser);
            if (isDisposed() || visibilityWindowListeners == null) return;
            org.eclipse.swt.browser.WindowEvent event = new org.eclipse.swt.browser.WindowEvent(chromium);
            event.display = chromium.getDisplay ();
            event.widget = chromium;
            event.size = new Point(0,0);
            event.location = new Point(0,0);
            if (isPopup != null) {
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

//              chromium.getDisplay().asyncExec(() -> {
                for (VisibilityWindowListener listener : visibilityWindowListeners) {
                    listener.show(event);
                }
//              });
                try {
                    // not sleeping here causes deadlock with multiple window.open
                    Thread.sleep(LOOP);
                } catch (InterruptedException e) {
                }
            }
//        });
    	} finally {
//    		ChromiumLib.lock.unlock();
    	}
    }

    int on_before_popup(long self, long browser, long frame,
		  long target_url, long target_frame_name, int target_disposition,
		  int user_gesture, long popupFeaturesPtr, long windowInfo,
		  long client, long settings, int no_javascript_access) {
    	debugPrint("on_before_popup " + browser);
		if (isDisposed()) 
		  return 1;
		if (openWindowListeners == null) 
		  return 0;
		loopDisable = true;
		pumpDisable = true;
		
		WindowEvent event = new WindowEvent(chromium);
		
		cef_popup_features_t popupFeatures = new cef_popup_features_t();
		ChromiumLib.memmove(popupFeatures, popupFeaturesPtr, ChromiumLib.cef_popup_features_t_sizeof());
		
//		try {
//            // not sleeping here causes deadlock with multiple window.open
//            Thread.sleep(LOOP);
//        } catch (InterruptedException e) {
//        }
		chromium.getDisplay().syncExec(() -> {
		  debugPrint("on_before_popup syncExec" + browser);
		  event.display = chromium.getDisplay ();
		  event.widget = chromium;
		  event.required = false;
		  event.addressBar = popupFeatures.locationBarVisible == 1;
		  event.menuBar = popupFeatures.menuBarVisible == 1;
		  event.statusBar = popupFeatures.statusBarVisible == 1;
		  event.toolBar = popupFeatures.toolBarVisible == 1;
		  int x = popupFeatures.xSet == 1 ? popupFeatures.x : 0 ;
		  int y = popupFeatures.ySet == 1 ? popupFeatures.y : 0 ;
		  event.location = new Point(x, y);
		  int width = popupFeatures.widthSet == 1 ? popupFeatures.width : 0;
		  int height = popupFeatures.heightSet == 1 ? popupFeatures.height : 0;
		  event.size = new Point(width, height);
		
		  for (OpenWindowListener listener : openWindowListeners) {
		      listener.open(event);
		  }
		  
		  if (event.browser != null) {
			  event.browser.webBrowser.createPopup(windowInfo, client, event);
		  } else {
			  createDefaultPopup(windowInfo, client, event);
		  }
		});
		loopDisable = false;
		
		if (event.browser == null && event.required)
		  return 1;
		return 0;
	}

    private void waitForClose(Display display) {
    	if (display == null || display.isDisposed()) return;
        display.asyncExec(() -> {
            if (browser != 0) {
                waitForClose(display);
            }
        });
    }
    
	private void set_load_handler() {
        loadHandler = CEFFactory.newLoadHandler();
        loadHandler.on_loading_state_change_cb = new Callback(this, "on_loading_state_change", void.class, new Type[] {long.class, long.class, int.class, int.class, int.class});
        loadHandler.on_loading_state_change = checkGetAddress(loadHandler.on_loading_state_change_cb);
        clientHandler.get_load_handler_cb = new Callback(this, "get_load_handler", long.class, new Type[] {long.class});
        clientHandler.get_load_handler = checkGetAddress(clientHandler.get_load_handler_cb);
        loadHandler.ptr = C.malloc (cef_load_handler_t.sizeof);
        ChromiumLib.memmove(loadHandler.ptr, loadHandler, cef_load_handler_t.sizeof);
    }
    
    long get_load_handler(long client) {
//        debugPrint("GetLoadHandler");
        if (loadHandler == null)
    		return 0;
        return loadHandler.ptr;
    }

    void on_loading_state_change(long self_, long browser, int isLoading, int canGoBack, int canGoForward) {
    	debugPrint("on_loading_state_change " + isLoading);
    	Chromium.this.canGoBack = canGoBack == 1;
    	Chromium.this.canGoForward = canGoForward == 1;
    	if (isDisposed() || progressListeners == null) return;
		updateText();
    	if (isPopup != null) {
    		textReady.thenRun(() -> enableProgress.complete(true));
    	}
    	else if (!enableProgress.isDone() && isLoading == 0) {
    		textReady.thenRun(() -> {
    			enableProgress.complete(true);
    		});
    		return;
    	}
    	else if (!enableProgress.isDone()) {
    		return;
    	}
    	ProgressEvent event = new ProgressEvent(chromium);
    	event.display = chromium.getDisplay ();
    	event.widget = chromium;
    	event.current = MAX_PROGRESS;
    	event.current = isLoading == 1 ? 1 : MAX_PROGRESS;
    	event.total = MAX_PROGRESS;
    	if (isLoading == 1) {
    		loadComplete = new CompletableFuture<>();
    		debugPrint("progress changed");
    		for (ProgressListener listener : progressListeners) {
    			listener.changed(event);
    		}
    	} else {
    		loadComplete.complete(true);
    		textReady.thenRun(() -> {
    			debugPrint("progress completed"); 
    			chromium.getDisplay().asyncExec(() -> {
    				for (ProgressListener listener : progressListeners) {
    					listener.completed(event);
    				}
    			});
    			
    		});
    	}
    }

    private void set_display_handler() {
        displayHandler = CEFFactory.newDisplayHandler();
        displayHandler.on_title_change_cb = new Callback(this, "on_title_change", void.class, new Type[] {long.class, long.class, long.class});
        displayHandler.on_title_change = checkGetAddress(displayHandler.on_title_change_cb);
        displayHandler.on_address_change_cb = new Callback(this, "on_address_change", void.class, new Type[] {long.class, long.class, long.class, long.class});
        displayHandler.on_address_change = checkGetAddress(displayHandler.on_address_change_cb);
        displayHandler.on_status_message_cb = new Callback(this, "on_status_message", void.class, new Type[] {long.class, long.class, long.class});
        displayHandler.on_status_message = checkGetAddress(displayHandler.on_status_message_cb);
        
        clientHandler.get_display_handler_cb = new Callback(this, "get_display_handler", long.class, new Type[] {long.class});
        clientHandler.get_display_handler = checkGetAddress(clientHandler.get_display_handler_cb);
        displayHandler.ptr = C.malloc (cef_display_handler_t.sizeof);
        ChromiumLib.memmove(displayHandler.ptr, displayHandler, cef_display_handler_t.sizeof);
    }
    
    long get_display_handler(long client) {
//        debugPrint("GetDisplayHandler");
        if (displayHandler == null)
    		return 0;
        return displayHandler.ptr;
    }
    
	void on_title_change(long self, long browser, long title) {
		if (isDisposed() || titleListeners == null) return;
		String full_str = ChromiumLib.cefswt_cefstring_to_java(title);
		String str = getPlainUrl(full_str);
		debugPrint("on_title_change: " + str);
		TitleEvent event = new TitleEvent(chromium);
		event.display = chromium.getDisplay ();
		event.widget = chromium;
		event.title = str;
		for (TitleListener listener : titleListeners) {
			listener.changed(event);
		}
	}
	
	void on_address_change(long self, long browser, long frame, long url) {
		//debugPrint("on_address_change");
		if (isDisposed() || locationListeners == null) return;
		LocationEvent event = new LocationEvent(chromium);
		event.display = chromium.getDisplay();
		event.widget = chromium;
		event.doit = true;
		event.location = getPlainUrl(ChromiumLib.cefswt_cefstring_to_java(url));
		event.top = ChromiumLib.cefswt_is_main_frame(frame);
		if (!enableProgress.isDone()) {
		    debugPrint("!on_address_change to " + event.location + " " + (event.top ? "main" : "!main"));
			return;
		}
		//if (!("about:blank".equals(event.location) && ignoreFirstEvents)) {
		    debugPrint("on_address_change to " + event.location + " " + (event.top ? "main" : "!main"));
		chromium.getDisplay().asyncExec(() -> {
			for (LocationListener listener : locationListeners) {
				listener.changed(event);
			}
		});
	}

    void on_status_message(long self, long browser, long status) {
        if (isDisposed() || statusTextListeners == null || status == 0) return;
        String str = ChromiumLib.cefswt_cefstring_to_java(status);
        StatusTextEvent event = new StatusTextEvent(chromium);
        event.display = chromium.getDisplay ();
        event.widget = chromium;
        event.text = str;
        for (StatusTextListener listener : statusTextListeners) {
            listener.changed(event);
        }
    }

    private void set_request_handler() {
        requestHandler = CEFFactory.newRequestHandler();
        requestHandler.on_before_browse_cb = new Callback(this, "on_before_browse", int.class, new Type[] {long.class, long.class, long.class, long.class, int.class});
        requestHandler.on_before_browse = checkGetAddress(requestHandler.on_before_browse_cb);

        clientHandler.get_request_handler_cb = new Callback(this, "get_request_handler", long.class, new Type[] {long.class});
        clientHandler.get_request_handler = checkGetAddress(clientHandler.get_request_handler_cb);
        requestHandler.ptr = C.malloc (cef_request_handler_t.sizeof);
        ChromiumLib.memmove(requestHandler.ptr, requestHandler, cef_request_handler_t.sizeof);
    }
    
    long get_request_handler(long client) {
//        debugPrint("GetRequestHandler");
        if (requestHandler == null)
    		return 0;
        return requestHandler.ptr;
    }
    
    int on_before_browse(long self, long browser, long frame, long request, int is_redirect) {
        if (isDisposed() || locationListeners == null) return 0;
        if (ChromiumLib.cefswt_is_main_frame(frame)) {
            LocationEvent event = new LocationEvent(chromium);
            event.display = chromium.getDisplay();
            event.widget = chromium;
            event.doit = true;
            event.location = ChromiumLib.cefswt_request_to_java(request);
            debugPrint("on_before_browse:" + event.location);
            try {
            	loopDisable = true;
            	for (LocationListener listener : locationListeners) {
            		listener.changing(event);
            	}
            } finally {
            	loopDisable = false;	            	
            }
            if (!event.doit) {
            	debugPrint("canceled nav, dependats:"+enableProgress.getNumberOfDependents());
            	enableProgress = new CompletableFuture<>();
            }
            return event.doit ? 0 : 1;
        }
        return 0;
    }

    private void set_jsdialog_handler() {
        if (!"gtk".equals(SWT.getPlatform())) {
            return;
        }
        jsDialogHandler = CEFFactory.newJsDialogHandler();
        jsDialogHandler.on_jsdialog_cb = new Callback(this, "on_jsdialog", int.class, new Type[] {long.class, long.class, long.class, int.class, long.class, long.class, long.class, int.class});
		jsDialogHandler.on_jsdialog = checkGetAddress(jsDialogHandler.on_jsdialog_cb);

		clientHandler.get_jsdialog_handler_cb = new Callback(this, "get_jsdialog_handler", long.class, new Type[] {long.class});
        clientHandler.get_jsdialog_handler = checkGetAddress(clientHandler.get_jsdialog_handler_cb);
        jsDialogHandler.ptr = C.malloc (cef_jsdialog_handler_t.sizeof);
        ChromiumLib.memmove(jsDialogHandler.ptr, jsDialogHandler, cef_jsdialog_handler_t.sizeof);
    }
    
    long get_jsdialog_handler(long client) {
        debugPrint("GetJSDialogHandler");
        if (jsDialogHandler == null)
    		return 0;
        return jsDialogHandler.ptr;
    }
    
    int on_jsdialog(long self_, long browser, long origin_url, int dialog_type, long message_text, long default_prompt_text, long callback, int suppress_message) {
        if (isDisposed()) return 0;
        
        int style = SWT.ICON_WORKING;
        switch (dialog_type) {
        case cef_jsdialog_handler_t.JSDIALOGTYPE_ALERT:
            style = SWT.ICON_INFORMATION;
            break;
        case cef_jsdialog_handler_t.JSDIALOGTYPE_CONFIRM:
            style = SWT.ICON_WARNING;
            break;
        case cef_jsdialog_handler_t.JSDIALOGTYPE_PROMPT:
            style = SWT.ICON_QUESTION | SWT.YES | SWT.NO;
            break;
        }
        String url = ChromiumLib.cefswt_cefstring_to_java(origin_url);
        String msg = ChromiumLib.cefswt_cefstring_to_java(message_text);
        String prompt = ChromiumLib.cefswt_cefstring_to_java(default_prompt_text);
        MessageBox box = new MessageBox(chromium.getShell(), style);
        box.setText(getPlainUrl(url));
        if (prompt != null) {
            box.setMessage(msg);
        } else {
            box.setMessage(msg);
        }
        int open = box.open();
        ChromiumLib.cefswt_dialog_close(callback, open == SWT.OK || open == SWT.YES ? 1 : 0, default_prompt_text);
        chromium.getShell().forceActive();
        return 1;
    }

    private void set_context_menu_handler() {
        contextMenuHandler = CEFFactory.newContextMenuHandler();
		contextMenuHandler.run_context_menu_cb = new Callback(this, "run_context_menu", int.class, new Type[] {long.class, long.class, long.class, long.class, long.class, long.class});
		contextMenuHandler.run_context_menu = checkGetAddress(contextMenuHandler.run_context_menu_cb);
        
		clientHandler.get_context_menu_handler_cb = new Callback(this, "get_context_menu_handler", long.class, new Type[] {long.class});
        clientHandler.get_context_menu_handler = checkGetAddress(clientHandler.get_context_menu_handler_cb);
        contextMenuHandler.ptr = C.malloc (cef_context_menu_handler_t.sizeof);
        ChromiumLib.memmove(contextMenuHandler.ptr, contextMenuHandler, cef_context_menu_handler_t.sizeof);
    }
    
    long get_context_menu_handler(long client) {
        debugPrint("GetContextMenuHandler");
        if (contextMenuHandler == null)
    		return 0;
        return contextMenuHandler.ptr;
    }

    int run_context_menu(long self, long browser, long frame, long params, long model, long callback) {
        debugPrint("run_context_menu");
        if (chromium.getMenu() != null) {
            chromium.getMenu().setVisible(true);
            ChromiumLib.cefswt_context_menu_cancel(callback);
            return 1;
        }
        return 0;
    }

    private void set_text_visitor() {
        textVisitor = CEFFactory.newStringVisitor();
        textVisitor.visit_cb = new Callback(this, "textVisitor_visit", void.class, new Type[] {long.class, long.class});
        textVisitor.visit = checkGetAddress(textVisitor.visit_cb);
        textVisitor.ptr = C.malloc(cef_string_visitor_t.sizeof);
        ChromiumLib.memmove(textVisitor.ptr, textVisitor, cef_string_visitor_t.sizeof);
    }

	void textVisitor_visit(long self, long cefString) {
		debugPrint("text visited");
		String newtext = cefString != 0 ? ChromiumLib.cefswt_cefstring_to_java(cefString) : null;
		if (newtext != null) {
		    text = newtext;
			debugPrint("text visited completed");
		    textReady.complete(text);
		} else {
			debugPrint("text visited null");
		}
	}
	
    private void set_focus_handler() {
        focusHandler = CEFFactory.newFocusHandler();
        focusHandler.on_got_focus_cb = new Callback(this, "on_got_focus", void.class, new Type[] {long.class, long.class});
        focusHandler.on_got_focus = checkGetAddress(focusHandler.on_got_focus_cb);
        focusHandler.on_set_focus_cb = new Callback(this, "on_set_focus", int.class, new Type[] {long.class, long.class, int.class});
        focusHandler.on_set_focus = checkGetAddress(focusHandler.on_set_focus_cb);
        focusHandler.on_take_focus_cb = new Callback(this, "on_take_focus", void.class, new Type[] {long.class, long.class, int.class});
        focusHandler.on_take_focus = checkGetAddress(focusHandler.on_take_focus_cb);

		clientHandler.get_focus_handler_cb = new Callback(this, "get_focus_handler", long.class, new Type[] {long.class});
        clientHandler.get_focus_handler = checkGetAddress(clientHandler.get_focus_handler_cb);
        focusHandler.ptr = C.malloc (cef_focus_handler_t.sizeof);
        ChromiumLib.memmove(focusHandler.ptr, focusHandler, cef_focus_handler_t.sizeof);
    }
    
    long get_focus_handler(long client) {
    	debugPrint("GetFocusHandler");
    	if (focusHandler == null)
    		return 0;
        return focusHandler.ptr;
    }
    
    void on_got_focus(long focusHandler, long browser_1) {
        debugPrint("CALLBACK OnGotFocus");
        hasFocus = true;
        if (chromium.getDisplay().getFocusControl() != null) {
            chromium.setFocus();
        }
        browserFocus(true);
    }

    int on_set_focus(long focusHandler, long browser_1, int focusSource) {
        debugPrint("CALLBACK OnSetFocus " + focusSource);
        if (ignoreFirstFocus) {
        	ignoreFirstFocus  = false;
        	return 1;
        }
        return 0;
    }
    
    void on_take_focus(long focusHandler, long browser_1, int next) {
        debugPrint("CALLBACK OnTakeFocus " + next);
        hasFocus = false;
        Control[] tabOrder = chromium.getParent().getTabList();
        if (tabOrder.length == 0)
            tabOrder = chromium.getParent().getChildren();
        int indexOf = Arrays.asList(tabOrder).indexOf(chromium);
        if (indexOf != -1) {
            int newIndex = (next == 1) ? indexOf + 1 : indexOf - 1;
            if (newIndex > 0 && newIndex < tabOrder.length && !tabOrder[newIndex].isDisposed()) {
                tabOrder[newIndex].setFocus();
                return;
            }
        }
        if (!isDisposed() && !chromium.getParent().isDisposed()) {
            chromium.getParent().setFocus();
        }
    }

    @Override
    public boolean isFocusControl() {
    	return hasFocus;
    }
    
    // single loop for all browsers
    private static void doMessageLoop(final Display display) {
        loopWork = new Runnable() {
            public void run() {
                if (lib != null && !display.isDisposed()) {
//                	debug("WORK CLOCK");
                	safe_loop_work();
                	display.timerExec(LOOP, loopWork);
                } else {
                    debug("STOPPING MSG LOOP");
                }
            }
        };
		display.timerExec(LOOP, loopWork);
    }
    
    private synchronized void checkBrowser() {
        if (lib == null) {
            SWT.error(SWT.ERROR_FAILED_LOAD_LIBRARY);
        }
        if (browser == 0) {
            SWT.error(SWT.ERROR_WIDGET_DISPOSED);
        }
    }

    private int on_process_message_received(long client, long browser, int source, long processMessage) {
        if (source != CEFFactory.PID_RENDERER || !jsEnabled || disposing == 1 || isDisposed()) {
            return 0;
        }
        FunctionSt fn = new FunctionSt();
		ChromiumLib.cefswt_function_id(processMessage, fn);
        int id = fn.id;
        if (id < 0) {
            return 0;
        }
        int argsSize = fn.args;
        Object[] args = new Object[argsSize];
        for (int i = 0; i < argsSize; i++) {
            int arg = i;
            EvalReturned callback = (loop, type, valuePtr) -> {
            	if (loop == 1) {
            		System.out.println("DISPATCH");
            		chromium.getDisplay().readAndDispatch();
            	} else {
        			String value = ChromiumLib.cefswt_cstring_to_java(valuePtr);
            		args[arg] = mapType(type, value);
            	}
            };
            Callback callback_cb = new Callback(callback, "invoke", void.class, new Type[] {int.class, int.class, long.class});
            ChromiumLib.cefswt_function_arg(processMessage, i, checkGetAddress(callback_cb));
            callback_cb.dispose();
        }
        Object ret = functions.get(id).function(args);
        
        Object[] returnPair = convertType(ret);
        ReturnType returnType = (ReturnType) returnPair[0];
        String returnStr = (String) returnPair[1];
		ChromiumLib.cefswt_function_return(browser, id, fn.port, returnType.intValue(), returnStr);
        
        return 1;
    }

    private Object[] convertType(Object ret) {
        ReturnType returnType = ReturnType.Error;
        String returnStr = "";
        if (ret == null) { 
            returnType = ReturnType.Null;
            returnStr = "null";
        } else if (Boolean.class.isInstance(ret)) {
            returnType = ReturnType.Bool;
            returnStr = Boolean.TRUE.equals(ret) ? "1" : "0";
        } else if (Number.class.isInstance(ret)) {
            returnType = ReturnType.Double;
            returnStr = NumberFormat.getInstance(Locale.US).format(ret);
        } else if (String.class.isInstance(ret)) {
            returnType = ReturnType.Str;
            returnStr = ret.toString();
        } else if (ret.getClass().isArray()) {
            returnType = ReturnType.Array;
            StringBuilder buffer = new StringBuilder();
            buffer.append("\"");
            for (int i = 0; i < Array.getLength(ret); i++) {
                if (i > 0) {
                    buffer.append(";");
                }
                Object[] arrayElem = convertType(Array.get(ret, i));
                buffer.append("'");
                buffer.append(((ReturnType) arrayElem[0]).intValue());
                buffer.append(",");
                buffer.append((String) arrayElem[1]);
                buffer.append("'");
            }
            buffer.append("\"");
            returnStr = buffer.toString();
        } else {
            returnStr = "Unsupported return type " + ret.getClass().getName();
        }
		return new Object[] {returnType, returnStr};
	}

	protected void browserFocus(boolean set) {
        //debugPrint("cef focus: " + set);
        if (!isDisposed() && browser != 0) {
            long parent = (Display.getDefault().getActiveShell() == null) ? 0 : getHandle(chromium.getParent());
            if (chromium.getDisplay().getActiveShell() != chromium.getShell()) {
//              System.err.println("Ignore do_message_loop_work due inactive shell");
                return;
            }
            ChromiumLib.cefswt_set_focus(browser, set, parent);
        }
    }

    public void dispose() {
    	if (disposing == 1 || isDisposed())
    		return;
    	boolean callClose = disposing != 2;
    	disposing = 1;
    	disposingAny++;
    	if (focusListener != null)
    		chromium.removeFocusListener(focusListener);
    	focusListener = null;
    	if (browser != 0 && callClose) {
//          browsers.decrementAndGet();
    		debugPrint("call close_browser");
    		ChromiumLib.cefswt_close_browser(browser);
    	}
    }

    /**
     * Re-initializing CEF3 is not supported due to the use of globals. This must be called on app exit. 
     */
    public static void shutdown() {
        String platform = SWT.getPlatform();
        if ("cocoa".equals(platform)) {
            // ignore on mac, will shutdown with shutdown hook, otherwise it crashes.
            return;
        }
        debug("Shutdown from API");
        internalShutdown();
    }
    
    private static synchronized void internalShutdown() {
        if (lib == null || app == null) {
            return;
        }
        if (browsers.get() == 0) {
            debug("shutting down CEF on exit from thread " + Thread.currentThread().getName());
            ChromiumLib.cefswt_shutdown();
            
            if (cookieVisitor != null) {
	            cookieVisitor.visit_cb.dispose();
	            C.free(cookieVisitor.ptr);
	            cookieVisitor = null;
            }
            
            app.get_browser_process_handler_cb.dispose();
            C.free(app.ptr);
            app = null;
            
            browserProcessHandler.on_schedule_message_pump_work_cb.dispose();
            C.free(browserProcessHandler.ptr);
            browserProcessHandler = null;
            //MemoryIO.getInstance().freeMemory(Struct.getMemory(app).address());
            debug("after shutting down CEF");
        } else if (!shuttindDown) {
            shuttindDown = true;
            debug("delaying shutdown due browsers not disposed yet");
        }
    }

    private static Object loadLib() {
        String platform = SWT.getPlatform();
        if ("gtk".equals(platform)) {
            String gtk = System.getProperty("org.eclipse.swt.internal.gtk.version", "");
            if (gtk.startsWith("2")) {
                throw new SWTException(SWT.ERROR_FAILED_LOAD_LIBRARY, "Chromium Browser is no longer supported in GTK2. ");
            }
        }

        String subDir = "chromium-" + CEFVERSION;
        File cefrustlib = null;
        try {
            String mapLibraryName = System.mapLibraryName(SHARED_LIB_V);
            String mapJniName = JNI_LIB_V;
            Enumeration<URL> fragments = Library.class.getClassLoader().getResources(subDir+"/chromium.properties");
            while (fragments.hasMoreElements()) {
                URL url = (URL) fragments.nextElement();
                try (InputStream is = url.openStream();) {
                    Properties props = new Properties();
                    props.load(is);
                    for (String prop : props.stringPropertyNames()) {
                        if (!"cefVersion".equals(prop)) {
                            String propValue = props.getProperty(prop);
                            Path path = Paths.get(propValue);
                            String fileName = path.getFileName().toString();
                            if (!mapLibraryName.equals(fileName) && !fileName.startsWith(mapJniName)) {
                                ResourceExpander.findResource(path.getParent().toString(), fileName, false);
                            }
                        }
                    }
                }
            }
            
            cefrustlib = ResourceExpander.findResource(subDir, mapLibraryName, false);
            File jnilib = ResourceExpander.findResource(subDir, mapJniName, true);
            
        	cefrustPath = cefrustlib.getParentFile().getCanonicalPath();
        
        	CEFFactory.create(cefrustPath);
        	Library.loadLibrary(cefrustlib.toString(), false);
        	Library.loadLibrary(jnilib.toString(), false);
            
            setupCookies();

            return new Object();
        } catch(UnsatisfiedLinkError e) {
            String cefLib = System.mapLibraryName("cef");
            if ("cocoa".equals(platform)) {
            	cefLib = "Chromium Embedded Framework.framework";
            }
            else if ("win32".equals(platform)) {
            	cefLib = "libcef.dll";
            }
			if (cefrustlib != null && !new File(cefrustlib.getParentFile(), cefLib).exists()) {
                SWTException swtError = new SWTException(SWT.ERROR_FAILED_LOAD_LIBRARY, "Missing CEF binaries for Chromium Browser. "
                        + "Extract CEF binaries to " + cefrustPath);
                swtError.throwable = e;
                throw swtError;
            }
            throw e;
        } catch (IOException e) {
            SWTException swtError = new SWTException(SWT.ERROR_FAILED_LOAD_LIBRARY, "");
            swtError.throwable = e;
            throw swtError;
        }
    }

    private static void setupCookies() {
        WebBrowser.NativeClearSessions = () -> {
            ChromiumLib.cefswt_delete_cookies();
        };
        WebBrowser.NativeSetCookie = () -> {
            List<HttpCookie> cookies = HttpCookie.parse(WebBrowser.CookieValue);
            for (HttpCookie cookie : cookies) {
                long age = cookie.getMaxAge();
                if (age != -1) {
                    age = Instant.now().plusSeconds(age).getEpochSecond();
                }
                WebBrowser.CookieResult = ChromiumLib.cefswt_set_cookie(WebBrowser.CookieUrl, 
                        cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), 
                        cookie.getSecure() ? 1 : 0, cookie.isHttpOnly() ? 1 : 0, age);
//                debug("CookieSet " + WebBrowser.CookieUrl + " " + cookie.getName() + " " + cookie.getValue() + " " + cookie.getDomain());
                break;
            }
        };
        WebBrowser.NativeGetCookie = () -> {
            if (cookieVisitor == null) {
                setCookieVisitor();
            }
            cookieVisited = new CompletableFuture<>();
            boolean result = ChromiumLib.cefswt_get_cookie(WebBrowser.CookieUrl, cookieVisitor.ptr);
            if (!result) {
                cookieVisited = null;
                throw new SWTException("Failed to get cookies");
            }
            try {
                cookieVisited.get(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                // no cookies found
            } finally {
                cookieVisited = null;
            }
        };
    }

    private static void setCookieVisitor() {
        cookieVisitor = CEFFactory.newCookieVisitor();
        cookieVisitor.visit_cb = new Callback(Chromium.class, "cookieVisitor_visit", int.class, new Type[] {long.class, long.class, int.class, int.class, int.class});
        cookieVisitor.visit = checkGetAddress(cookieVisitor.visit_cb);
        
        cookieVisitor.ptr = C.malloc(cef_cookie_visitor_t.sizeof);
        ChromiumLib.memmove(cookieVisitor.ptr, cookieVisitor, cef_cookie_visitor_t.sizeof);
    }
    
    static int cookieVisitor_visit(long self, long cefcookie, int count, int total, int delete) {
    	String name = ChromiumLib.cefswt_cookie_to_java(cefcookie);
        debug("Visitor " + count + "/" +total + ": " + name + ":" + Thread.currentThread());
        if (WebBrowser.CookieName != null && WebBrowser.CookieName.equals(name)) {
            String value = ChromiumLib.cefswt_cookie_value(cefcookie);
            debug("cookie value: " + value);
            WebBrowser.CookieValue = value;
            cookieVisited.complete(true);
            return 0;
        }
        return 1;
    }

    private final class CefFocusListener implements FocusListener {
        private boolean enabled = true;
        
        @Override
        public void focusLost(FocusEvent e) {
            if (!enabled)
                return;
            enabled = false;
            //debugPrint("focusLost");
            browserFocus(false);
            // System.out.println(Display.getDefault().getFocusControl());
            enabled = true;
        }

        @Override
        public void focusGained(FocusEvent e) {
            if (!enabled)
                return;
            //debugPrint("focusGained");
            browserFocus(true);
        }
    }
    
    @Override
    public boolean back() {
    	if (lib == null) {
            SWT.error(SWT.ERROR_FAILED_LOAD_LIBRARY);
        }
        if (canGoBack) {
            ChromiumLib.cefswt_go_back(browser);
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
        	ChromiumLib.cefswt_execute(browser, script);
        });
        return true;
    }
    
    @Override
    public Object evaluate(String script) throws SWTException {
    	if (lib == null) {
            SWT.error(SWT.ERROR_FAILED_LOAD_LIBRARY);
        }
    	if (!jsEnabled) {
    		return null;
    	}
    	if (browser == 0) {
        	if (paintListener != null) {
        		chromium.removePaintListener(paintListener);
        		paintListener = null;
        		createBrowser();
        	}
    	}
        Object[] ret = new Object[1];
        EvalReturned callback = (loop, type, valuePtr) -> {
        	if (loop == 1) {
        		debugPrint("eval retured: " +type + ":"+valuePtr);
        		if (!(loopDisable && ("cocoa".equals(SWT.getPlatform()) || "gtk".equals(SWT.getPlatform())))) {
        			chromium.getDisplay().readAndDispatch();
        		}
        		if (!loopDisable) {
//        			lib.cefswt_do_message_loop_work();
        		}
        	} else {
    			String value = ChromiumLib.cefswt_cstring_to_java(valuePtr);
    			debugPrint("eval returned: " +type +":"+value);
        		ret[0] = mapType(type, value);
        	}
        };
        Callback callback_cb = new Callback(callback, "invoke", void.class, new Type[] {int.class, int.class, long.class});
        
        StringBuilder buffer = new StringBuilder ("(function() {");
        buffer.append ("\n");
        buffer.append (script);
        buffer.append ("\n})()");
        
        checkBrowser();
        boolean returnSt = ChromiumLib.cefswt_eval(browser, buffer.toString(), EVAL++, checkGetAddress(callback_cb));
        callback_cb.dispose();
        if (!returnSt) {
            throw new SWTException("Script that was evaluated failed");
        }
        return ret[0];
    }

    private Object mapType(int type, String value) throws SWTException {
        if (type == ReturnType.Error.intValue()) {
              if ((SWT.ERROR_INVALID_RETURN_VALUE+"").equals(value)) {
                  throw new SWTException(SWT.ERROR_INVALID_RETURN_VALUE);
              }
              throw new SWTException(SWT.ERROR_FAILED_EVALUATE, value);
          } 
          else if (type == ReturnType.Null.intValue()) {
              return null;
          } 
          else if (type == ReturnType.Bool.intValue()) {
              return "1".equals(value) ? Boolean.TRUE : Boolean.FALSE ;
          } 
          else if (type == ReturnType.Double.intValue()) {
              return Double.parseDouble(value);
          } 
          else if (type == ReturnType.Array.intValue()) {
        	  String value_unquoted = value.substring(1, value.length()-1);
        	  String[] elements = value_unquoted.split(";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
              Object[] array = new Object[elements.length];
              for (int i = 0; i < array.length; i++) {
            	  String elemUnquoted = elements[i].substring(1, elements[i].length()-1);
            	  String[] parts = elemUnquoted.split(",(?=(?:[^']*'[^']*')*[^']*$)", 2);
            	  ReturnType elemType = CEFFactory.ReturnType.from(parts[0]);
            	  Object elemValue = mapType(elemType.intValue(), parts[1]);
            	  array[i] = elemValue;
              }
              return array;
          } 
          else {
              return value;
          }
    }

    @Override
    public boolean forward() {
    	if (lib == null) {
            SWT.error(SWT.ERROR_FAILED_LOAD_LIBRARY);
        }
    	if (canGoForward) {
    		ChromiumLib.cefswt_go_forward(browser);
            return true;
        }
        return false;
    }

    @Override
    public String getBrowserType() {
        return "chromium";
    }

    @Override
    public String getText() {
        checkBrowser();
        return text;
    }
    
    private void updateText() {
        if (browser != 0 && textVisitor != null) {
            debugPrint("update text");
            textReady = new CompletableFuture<String>();
            ChromiumLib.cefswt_get_text(browser, textVisitor.ptr);
        }
    }

    @Override
    public String getUrl() {
    	if (lib == null) {
            SWT.error(SWT.ERROR_FAILED_LOAD_LIBRARY);
        }
    	if (browser == 0) {
            if (this.url == null) {
                return "about:blank";
            }
    		return getPlainUrl(this.url);
    	}
        String cefurl = ChromiumLib.cefswt_get_url(browser);
//        debugPrint("getUrl1:" + cefurl);
        if (cefurl == null)
            cefurl = getPlainUrl(this.url);
        return cefurl;
    }

    @Override
    public boolean isBackEnabled() {
        return canGoBack;
    }

    @Override
    public boolean isForwardEnabled() {
        return canGoForward;
    }

    @Override
    public void refresh() {
    	if (lib == null) {
            SWT.error(SWT.ERROR_FAILED_LOAD_LIBRARY);
        }
    	jsEnabled = jsEnabledOnNextPage;
        if (browser != 0) {
        	ChromiumLib.cefswt_reload(browser);
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
    	this.url = url;
    	this.postData = postData;
    	this.headers = headers;
        jsEnabled = jsEnabledOnNextPage;
        if (!isDisposed() && browser != 0) {
            debugPrint("set url: " + url);
            doSetUrl(url, postData, headers);
        }
        return true;
    }

	private CompletableFuture<Void> doSetUrl(String url, String postData, String[] headers) {
		return enableProgress.thenRun(() -> {
			debugPrint("load url");
			doSetUrlPost(browser, url, postData, headers);
		});
	}

	private void doSetUrlPost(long browser, String url, String postData, String[] headers) {
		byte[] bytes = (postData != null) ? postData.getBytes(Charset.forName("ASCII")) : null;
		int bytesLength = (postData != null) ? bytes.length : 0 ;
		int headersLength = (headers != null) ? headers.length : 0 ; 
		String joinHeaders = headers == null ? null : String.join("::", headers);
		ChromiumLib.cefswt_load_url(browser, url, bytes, bytesLength, joinHeaders, headersLength);
	}
    
    @Override
    public void stop() {
    	if (lib == null) {
            SWT.error(SWT.ERROR_FAILED_LOAD_LIBRARY);
        }
    	if (browser != 0) {
    		ChromiumLib.cefswt_stop(browser);
    	}
    }
    
    boolean isDisposed() {
    	return chromium == null || chromium.isDisposed();
    }
    
    static int cbs = 0;
    static long checkGetAddress(Callback cb) {
    	long address = cb.getAddress();
    	cbs++;
    	if (address == 0) {
    		throw new SWTError(SWT.ERROR_NO_HANDLES);
    	}
//    	debug("CALLBACKS "+cbs);
		return address;
    }
    
    static void disposeCallback(Callback cb) {
    	if (cb == null) {
    		System.err.println("NULL CALLBACK ");
    	}
    	if (cb != null) {
    		cb.dispose();
    	}
    }

}
