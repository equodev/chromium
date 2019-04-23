package org.eclipse.swt.chromium;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Widget;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.Direct;
import jnr.ffi.annotations.Encoding;
import jnr.ffi.provider.ClosureManager;
import jnr.ffi.provider.jffi.NativeRuntime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpCookie;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.internal.Library;
import org.eclipse.swt.internal.chromium.CEF;
import org.eclipse.swt.internal.chromium.CEFFactory;
import org.eclipse.swt.internal.chromium.CEFFactory.EvalReturned;
import org.eclipse.swt.internal.chromium.CEFFactory.FunctionSt;
import org.eclipse.swt.internal.chromium.CEFFactory.ReturnType;
import org.eclipse.swt.internal.chromium.ResourceExpander;

class Chromium extends WebBrowser {
	private static final String DATA_TEXT_URL = "data:text/html;base64,";
	private static final String VERSION = "0801";
    private static final String CEFVERSION = "3071";
    private static final String SHARED_LIB_V = "chromium_swt-"+VERSION;
    private static final int MAX_PROGRESS = 100;
    private static final int LOOP = 50;
//    private static final String SUBP = "chromium_subp";
//    private static final String SUBP_V = "chromium_subp-"+VERSION;
    
    Browser chromium;
    OpenWindowListener[] openWindowListeners = new OpenWindowListener[0];

    static {
        lib = loadLib();
    }
    
    private static Lib lib;
    private static String cefrustPath;
    private static AtomicInteger browsers = new AtomicInteger(0);
    private static CompletableFuture<Boolean> cefInitilized;
    private static CEF.cef_app_t app;
    private static CEF.cef_browser_process_handler_t browserProcessHandler;
    private static boolean shuttindDown;
    private static CEF.cef_cookie_visitor_t cookieVisitor;
    private static CompletableFuture<Boolean> cookieVisited;
    private static int EVAL = 1;
    private static int INSTANCES = 0;
    private static Runnable loopWork;
    private static boolean loopDisable;
    private static boolean pumpDisable;
    private static int disposingAny = 0;

    private long hwnd;
    private Pointer browser;
    private CEF.cef_client_t clientHandler;
    private CEF.cef_focus_handler_t focusHandler;
    private CEF.cef_life_span_handler_t lifeSpanHandler;
    private CEF.cef_load_handler_t loadHandler;
    private CEF.cef_display_handler_t displayHandler;
    private CEF.cef_request_handler_t requestHandler;
    private CEF.cef_jsdialog_handler_t jsDialogHandler;
    private CEF.cef_context_menu_handler_t contextMenuHandler;
    private CEF.cef_string_visitor_t  textVisitor;
    private FocusListener focusListener;
    private String url;
    private String postData;
    private String[] headers;
    private String text = "";
    private CompletableFuture<String> textReady;
    private boolean canGoBack;
    private boolean canGoForward;
    private CompletableFuture<Boolean> enableProgress = new CompletableFuture<>();
    private CompletableFuture<Boolean> created = new CompletableFuture<>();
    private boolean disposing;
    private int instance;
	private boolean hasFocus;
	private boolean ignoreFirstFocus = true;
	private PaintListener paintListener;
	private WindowEvent isPopup;
    private List<jnr.ffi.Struct> popupHandlers = new ArrayList<>();

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
    		
    		if (!lib.cefswt_function(browser, function.name, function.index)) {
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
        debugPrint("initCef Done");
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

    private jnr.ffi.Pointer debugPrint(String log) {
        System.out.println("J"+instance + ":" + Thread.currentThread().getName() +":" + log + (this.url != null ? " (" + getPlainUrl(this.url) + ")" : " empty-url"));
        return null;
    }
    
    private static jnr.ffi.Pointer debug(String log) {
        System.out.println("J:" + log);
        return null;
    }

    private void initCEF(Display display) {
        synchronized (lib) {
            if (app == null) {
                CEFFactory.create();
                app = CEFFactory.newApp();
                browserProcessHandler = CEFFactory.newBrowserProcessHandler();
                cefInitilized = new CompletableFuture<>();
                browserProcessHandler.on_context_initialized.set(browserProcessHandler -> {
                    debugPrint("OnContextInitialized");
                    
                    cefInitilized.complete(true);
                });
                Runnable runnable = () -> { 
                    if (lib == null || display.isDisposed() /*|| display.getActiveShell() != getShell()*/) {
                        //System.err.println("Ignore do_message_loop_work due inactive shell");
                        return;
                    }
//                    debug("WORK PUMP");
                    safe_loop_work();
                };
                browserProcessHandler.on_schedule_message_pump_work.set((pbrowserProcessHandler, delay) -> {
                    if (lib == null || display.isDisposed() || browsers.get() <= 0 || pumpDisable || disposingAny > 0)
                        return;
//                    debugPrint("pump "+delay);
                    Runnable scheduleWork = () -> {
                    	restartLoop(display, delay);
                    	display.timerExec(-1, runnable);
//    	                        debug("WORK PUMP DELAYED");
                    	display.timerExec(delay, runnable);
                    };
                    if (Display.getCurrent() != null) {
                    	if (delay <= 0) {
                    		restartLoop(display, 0);
//                        	debug("WORK PUMP NOW");
                			display.asyncExec(runnable);
                    	} else {
                    		scheduleWork.run();
                    	}
                    } else {
                    	if (delay <= 0) {
                    		display.asyncExec(() -> {
                    			restartLoop(display, 0);
//                                debug("WORK PUMP ALMOST NOW");
    	                        runnable.run();
                    		});
                    	} else {
							display.asyncExec(scheduleWork);
                    	}
                    }
                });
                
                app.get_browser_process_handler.set(appPtr -> {
//                    debugPrint("GetBrowserProcessHandler");
                    return browserProcessHandler;
                });
                System.out.println("cefrust.path: " + cefrustPath);
                int debugPort = 0;
                try {
                	debugPort = Integer.parseInt(System.getProperty("org.eclipse.swt.chromium.remote-debugging-port", "0"));
                } catch (NumberFormatException e) {
                	debugPort = 0;
                }
                lib.cefswt_init(app, cefrustPath, VERSION, debugPort);
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

    private static void safe_loop_work() {
        if (browsers.get() > 0 && !loopDisable) {
        	if (lib.cefswt_do_message_loop_work() == 0) {
        	    System.err.println("error looping");
        	}
        	if (pumpDisable == true) {
        	    pumpDisable = false;
        	}
        }
    }

	private void restartLoop(Display display, int ms) {
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
        initializeClientHandler(clientHandler);
        set_focus_handler();
        set_life_span_handler();
        set_load_handler();
        set_display_handler();
        set_request_handler();
        set_jsdialog_handler();
        set_context_menu_handler();
        clientHandler.on_process_message_received.set((c, browser_1, source, processMessage) -> {
            return browserFunctionCalled(source, processMessage);
        });

        chromium.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                if (!chromium.isDisposed() && browser != null) {
                    Point size = getChromiumSize();
                    debugPrint("size: " + size);
					lib.cefswt_resized(browser,  size.x,  size.y);
                }
            }
        });
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
        lib.cefswt_create_browser(hwnd, url, clientHandler, size.x, size.y, jsEnabledOnNextPage ? 1 : 0, cefBgColor);
    }

    private void createPopup(Pointer windowInfo, Pointer client, WindowEvent event) {
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

    	lib.cefswt_set_window_info_parent(windowInfo, client, clientHandler, popupHandle, 0, 0, size.x, size.y);
    	debugPrint("reparent popup");
	}
    
    private void createDefaultPopup(Pointer windowInfo, Pointer client, WindowEvent event) {
//    	CEF.cef_client_t nullHandler = null;
        CEF.cef_client_t nullHandler = CEFFactory.newClient();
        initializeClientHandler(nullHandler);
        CEF.cef_life_span_handler_t popupHandler = CEFFactory.newLifeSpanHandler();
        popupHandler.on_after_created.set((self, browser) -> {
            debug("popup on_after_created");
            try {
                // not sleeping here causes deadlock with multiple window.open
                Thread.sleep(LOOP);
            } catch (InterruptedException e) {
            }
        });
        popupHandler.on_before_close.set((plifeSpanHandler, browser) -> {
            debug("popup OnBeforeClose");
            popupHandlers.remove(nullHandler);
            popupHandlers.remove(popupHandler);
            disposingAny--;
        });
        popupHandler.do_close.set((plifeSpanHandler, browser) -> {
            debug("popup DoClose");
            disposingAny++;
            return 0;
        });

        nullHandler.get_life_span_handler.set(self -> {
            return popupHandler;
        });
        popupHandlers.add(popupHandler);
        popupHandlers.add(nullHandler);
		lib.cefswt_set_window_info_parent(windowInfo, client, nullHandler, 0, event.location.x, event.location.y, event.size.x, event.size.y);
    	debugPrint("default popup");
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
        lifeSpanHandler.on_before_close.set((plifeSpanHandler, browser) -> {
            debugPrint("OnBeforeClose");
            lib.cefswt_free(browser);
            Chromium.this.browser = null;
            Chromium.this.clientHandler = null;
            Chromium.this.focusHandler = null;
            Chromium.this.lifeSpanHandler = null;
            Chromium.this.loadHandler = null;
            Chromium.this.displayHandler = null;
            Chromium.this.requestHandler = null;
            Chromium.this.jsDialogHandler = null;
            Chromium.this.contextMenuHandler = null;
            // not always called on linux
            disposingAny--;
            if (browsers.decrementAndGet() == 0 && shuttindDown) {
                internalShutdown();
            }
        });
        lifeSpanHandler.do_close.set((plifeSpanHandler, browser) -> {
            //lifeSpanHandler.base.ref++;
            if (!lib.cefswt_is_same(Chromium.this.browser, browser)) {
            	debugPrint("DoClose popup:" + Chromium.this.browser+":"+browser);
            	return 0;
            }
            debugPrint("DoClose");
            if (!disposing && !chromium.isDisposed() && closeWindowListeners != null) {
                org.eclipse.swt.browser.WindowEvent event = new org.eclipse.swt.browser.WindowEvent(chromium);
                event.display = chromium.getDisplay ();
                event.widget = chromium;
//                event.browser = chromium;
                for (CloseWindowListener listener : closeWindowListeners) {
                    listener.close(event);
                }
            }
            
            if (!disposing) {
                chromium.dispose();
            }
            if ("gtk".equals(SWT.getPlatform())) {
            	waitForClose(chromium.getDisplay());
            }
            // do not send close notification to top level window
            // returning 0, cause the window to close 
            return 1;
        });
        lifeSpanHandler.on_after_created.set((self, browser) -> {
            if (chromium.isDisposed() || visibilityWindowListeners == null) return;
            debugPrint("on_after_created " + browser);
            if (browser != null) {
            	Chromium.this.browser = browser;
                browsers.incrementAndGet();
                if (this.isPopup == null) {
                	final org.eclipse.swt.graphics.Point size = getChromiumSize();
                	lib.cefswt_resized(browser, size.x,  size.y);
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
            
//            chromium.getDisplay().asyncExec(() -> {
                debugPrint("on_after_created handling " + browser);
                if (chromium == null || chromium.isDisposed() || visibilityWindowListeners == null) return;
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

                    for (VisibilityWindowListener listener : visibilityWindowListeners) {
                    	listener.show(event);
                    }
                    try {
                        // not sleeping here causes deadlock with multiple window.open
                        Thread.sleep(LOOP);
                    } catch (InterruptedException e) {
                    }
                }
//            });
        });
        lifeSpanHandler.on_before_popup.set((self, browser, frame,
                target_url, target_frame_name, target_disposition,
                user_gesture, popupFeatures, windowInfo,
                client, settings, no_javascript_access) -> {
            debugPrint("on_before_popup " + browser);
            if (chromium.isDisposed()) 
                return 1;
            if (openWindowListeners == null) 
                return 0;
            loopDisable = true;
            pumpDisable = true;

            WindowEvent event = new WindowEvent(chromium);
            
            chromium.getDisplay().syncExec(() -> {
                debugPrint("on_before_popup syncExec" + browser);
                event.display = chromium.getDisplay ();
                event.widget = chromium;
                event.required = false;
                event.addressBar = popupFeatures.locationBarVisible.get() == 1;
                event.menuBar = popupFeatures.menuBarVisible.get() == 1;
                event.statusBar = popupFeatures.statusBarVisible.get() == 1;
                event.toolBar = popupFeatures.toolBarVisible.get() == 1;
                int x = popupFeatures.xSet.get() == 1 ? popupFeatures.x.get() : 0 ;
                int y = popupFeatures.ySet.get() == 1 ? popupFeatures.y.get() : 0 ;
                event.location = new Point(x, y);
                int width = popupFeatures.widthSet.get() == 1 ? popupFeatures.width.get() : 0;
                int height = popupFeatures.heightSet.get() == 1 ? popupFeatures.height.get() : 0;
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
        });
        clientHandler.get_life_span_handler.set(client -> {
            //DEBUG_CALLBACK("GetLifeSpanHandler");
            return lifeSpanHandler;
        });
    }

    private void waitForClose(Display display) {
        display.asyncExec(() -> {
            if (browser != null) {
                waitForClose(display);
            }
        });
    }
    
	private void set_load_handler() {
        loadHandler = CEFFactory.newLoadHandler();
        loadHandler.on_loading_state_change.set((self_, browser, isLoading, canGoBack, canGoForward) -> {
            debugPrint("on_loading_state_change " + isLoading);
            Chromium.this.canGoBack = canGoBack == 1;
            Chromium.this.canGoForward = canGoForward == 1;
            if (chromium.isDisposed() || progressListeners == null) return;
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
                debugPrint("progress changed");
                for (ProgressListener listener : progressListeners) {
                    listener.changed(event);
                }
            } else {
                textReady.thenRun(() -> {
                	debugPrint("progress completed"); 
                    chromium.getDisplay().asyncExec(() -> {
                    	for (ProgressListener listener : progressListeners) {
                    		listener.completed(event);
                    	}
                    });
                });
            }
        });
        loadHandler.on_load_end.set((self, browser, frame, http_status) -> {
//        	debugPrint("on_load_end"); 
//        	if (chromium.isDisposed() || progressListeners == null) return;
        });
        clientHandler.get_load_handler.set(client -> {
            return loadHandler;
        });
    }

    private void set_display_handler() {
        displayHandler = CEFFactory.newDisplayHandler();
        displayHandler.on_title_change.set((self, browser, title) -> {
            if (chromium.isDisposed() || titleListeners == null) return;
            String str = getPlainUrl(lib.cefswt_cefstring_to_java(title));
            debugPrint("on_title_change: " + str);
            TitleEvent event = new TitleEvent(chromium);
            event.display = chromium.getDisplay ();
            event.widget = chromium;
            event.title = str;
            for (TitleListener listener : titleListeners) {
                listener.changed(event);
            }
        });
        displayHandler.on_address_change.set((self, browser, frame, url) -> {
//        	debugPrint("on_address_change");
        	if (chromium.isDisposed() || locationListeners == null) return;
            LocationEvent event = new LocationEvent(chromium);
            event.display = chromium.getDisplay();
            event.widget = chromium;
            event.doit = true;
            event.location = getPlainUrl(lib.cefswt_cefstring_to_java(url));
            event.top = lib.cefswt_is_main_frame(frame);
            if (!enableProgress.isDone()) {
                debugPrint("!on_address_change to " + event.location + " " + (event.top ? "main" : "!main"));
            	return;
            }
//            if (!("about:blank".equals(event.location) && ignoreFirstEvents)) {
                debugPrint("on_address_change to " + event.location + " " + (event.top ? "main" : "!main"));
            chromium.getDisplay().asyncExec(() -> {
            	for (LocationListener listener : locationListeners) {
            		listener.changed(event);
            	}
            });    
//            }
        });
        displayHandler.on_status_message.set((self, browser, status) -> {
            if (chromium.isDisposed() || statusTextListeners == null) return;
            String str = lib.cefswt_cefstring_to_java(status);
            StatusTextEvent event = new StatusTextEvent(chromium);
            event.display = chromium.getDisplay ();
            event.widget = chromium;
            event.text = str;
            for (StatusTextListener listener : statusTextListeners) {
                listener.changed(event);
            }
        });
        clientHandler.get_display_handler.set(client -> {
            return displayHandler;
        });
    }
    
    private void set_request_handler() {
        requestHandler = CEFFactory.newRequestHandler();
        requestHandler.on_before_browse.set((self, browser, frame, request, is_redirect) -> {
            if (chromium.isDisposed() || locationListeners == null) return 0;
            if (lib.cefswt_is_main_frame(frame)) {
	            LocationEvent event = new LocationEvent(chromium);
	            event.display = chromium.getDisplay();
	            event.widget = chromium;
	            event.doit = true;
	            event.location = lib.cefswt_request_to_java(request);
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
        });
        clientHandler.get_request_handler.set(client -> {
            return requestHandler;
        });
    }
    
    private void set_jsdialog_handler() {
        if (!"gtk".equals(SWT.getPlatform())) {
            return;
        }
        jsDialogHandler = CEFFactory.newJsDialogHandler();
        jsDialogHandler.on_jsdialog.set((self_, browser, origin_url, dialog_type, message_text, default_prompt_text, callback, suppress_message) -> {
            if (chromium.isDisposed()) return 0;
            
            int style = SWT.ICON_WORKING;
            switch (dialog_type) {
            case JSDIALOGTYPE_ALERT:
                style = SWT.ICON_INFORMATION;
                break;
            case JSDIALOGTYPE_CONFIRM:
                style = SWT.ICON_WARNING;
                break;
            case JSDIALOGTYPE_PROMPT:
                style = SWT.ICON_QUESTION | SWT.YES | SWT.NO;
                break;
            }
            String url = lib.cefswt_cefstring_to_java(origin_url);
            String msg = lib.cefswt_cefstring_to_java(message_text);
            String prompt = lib.cefswt_cefstring_to_java(default_prompt_text);
            MessageBox box = new MessageBox(chromium.getShell(), style);
            box.setText(getPlainUrl(url));
            if (prompt != null) {
                box.setMessage(msg);
            } else {
                box.setMessage(msg);
            }
            int open = box.open();
            lib.cefswt_dialog_close(callback, open == SWT.OK || open == SWT.YES ? 1 : 0, default_prompt_text);
            return 1;
        });
        clientHandler.get_jsdialog_handler.set(client -> {
            return jsDialogHandler;
        });
    }
    
    private void set_context_menu_handler() {
        contextMenuHandler = CEFFactory.newContextMenuHandler();
        contextMenuHandler.on_before_context_menu.set((self, browser, frame, params, model) -> {
            debugPrint("on_before_context_menu");
        });
        contextMenuHandler.run_context_menu.set((self, browser, frame, params, model, callback) -> {
            debugPrint("run_context_menu");
            if (chromium.getMenu() != null) {
                chromium.getMenu().setVisible(true);
                lib.cefswt_context_menu_cancel(callback);
                return 1;
            }
            return 0;
        });
        clientHandler.get_context_menu_handler.set(client -> {
            return contextMenuHandler;
        });
    }
    
    private void set_text_visitor() {
        textVisitor = CEFFactory.newStringVisitor();
        textVisitor.visit.set((self, cefString) -> {
//        	debugPrint("text visited");
            String newtext = cefString != null ? lib.cefswt_cefstring_to_java(cefString) : null;
            if (newtext != null) {
                text = newtext;
            	debugPrint("text visited completed");
                textReady.complete(text);
            } else {
            	debugPrint("text visited null");
            }
        });
    }

    private void set_focus_handler() {
        focusHandler = CEFFactory.newFocusHandler();
        focusHandler.on_got_focus.set((focusHandler, browser_1) -> {
            debugPrint("CALLBACK OnGotFocus");
            hasFocus = true;
            if (chromium.getDisplay().getFocusControl() != null) {
                chromium.setFocus();
            }
            browserFocus(true);
        });
        focusHandler.on_set_focus.set((focusHandler, browser_1, focusSource) -> {
            debugPrint("CALLBACK OnSetFocus " + focusSource);
            if (ignoreFirstFocus) {
            	ignoreFirstFocus  = false;
            	return 1;
            }
            return 0;
        });
        focusHandler.on_take_focus.set((focusHandler, browser_1, next) -> {
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
            if (!chromium.getParent().isDisposed()) {
                chromium.getParent().setFocus();
            }
        });
        clientHandler.get_focus_handler.set(client -> {
//            debugPrint("GetFocusHandler");
            return focusHandler;
        });
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
                	//debug("WORK CLOCK");
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
        if (browser == null) {
            SWT.error(SWT.ERROR_WIDGET_DISPOSED);
        }
    }

    protected void initializeClientHandler(CEF.cef_client_t client) {
        // callbacks
        client.get_context_menu_handler.set((c) -> null);
        client.get_dialog_handler.set((c) -> debug("get_dialog_handler"));
        client.get_download_handler.set((c) -> debug("get_download_handler"));
        client.get_drag_handler.set((c) -> debug("get_drag_handler"));
        client.get_geolocation_handler.set((c) -> debug("get_geolocation_handler"));
        client.get_jsdialog_handler.set((c) -> null);
        client.get_keyboard_handler.set((c) -> null);
        client.get_render_handler.set((c) -> null);
        client.on_process_message_received.set((c, browser_1, source, processMessage) -> {
            debug("on_process_message_received"); return 0;
        });
        client.get_find_handler.set(c -> debug("get_find_handler"));
    }

    private int browserFunctionCalled(CEF.cef_process_id_t source, Pointer processMessage) {
        if (source != CEF.cef_process_id_t.PID_RENDERER || !jsEnabled || disposing || chromium == null || chromium.isDisposed()) {
            return 0;
        }
        FunctionSt fn = lib.cefswt_function_id(processMessage);
        int id = fn.id.get();
        if (id < 0) {
            return 0;
        }
        int argsSize = fn.args.intValue();
        Object[] args = new Object[argsSize];
        for (int i = 0; i < argsSize; i++) {
            int arg = i;
            EvalReturned callback = (loop, type, value) -> {
            	if (loop == 1) {
            		System.out.println("DISPATCH");
            		chromium.getDisplay().readAndDispatch();
            	} else {
            		args[arg] = mapType(type, value);
            	}
            };
            lib.cefswt_function_arg(processMessage, i, callback);
        }
        Object ret = functions.get(id).function(args);
        
        Object[] returnPair = convertType(ret);
        ReturnType returnType = (ReturnType) returnPair[0];
        String returnStr = (String) returnPair[1];
		lib.cefswt_function_return(browser, id, fn.port.get(), returnType, returnStr);
        
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
            Object[] array = (Object[]) ret;
            StringBuilder buffer = new StringBuilder();
            buffer.append("\"");
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    buffer.append(";");
                }
                Object[] arrayElem = convertType(array[i]);
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
        if (!chromium.isDisposed() && browser != null) {
            long parent = (Display.getDefault().getActiveShell() == null) ? 0 : getHandle(chromium.getParent());
            if (chromium.getDisplay().getActiveShell() != chromium.getShell()) {
//              System.err.println("Ignore do_message_loop_work due inactive shell");
                return;
            }
            lib.cefswt_set_focus(browser, set, parent);
        }
    }

    public void dispose() {
        if (disposing || chromium.isDisposed())
            return;
        disposing = true;
        disposingAny++;
        if (focusListener != null)
            chromium.removeFocusListener(focusListener);
        focusListener = null;
        if (browser != null) {
//          browsers.decrementAndGet();
            debugPrint("call close_browser");
            lib.cefswt_close_browser(browser);
        }
        chromium = null;
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
            app = null;
            debug("shutting down CEF on exit from thread " + Thread.currentThread().getName());
            lib.cefswt_shutdown();
            //MemoryIO.getInstance().freeMemory(Struct.getMemory(app).address());
            debug("after shutting down CEF");
        } else if (!shuttindDown) {
            shuttindDown = true;
            debug("delaying shutdown due browsers not disposed yet");
        }
    }

    private static Lib loadLib() {
        fixJNRClosureClassLoader();
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
                            if (!mapLibraryName.equals(fileName)) {
                                ResourceExpander.findResource(path.getParent().toString(), fileName, false);
                            }
                        }
                    }
                }
            }
            
            cefrustlib = ResourceExpander.findResource(subDir, mapLibraryName, false);
        	cefrustPath = cefrustlib.getParentFile().getCanonicalPath();
        
            LibraryLoader<Lib> loader = LibraryLoader.create(Lib.class);
            Lib libc = loader
                .failImmediately()
                .search(cefrustPath)
                .load(SHARED_LIB_V);
            
            setupCookies();

            return libc;
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
            lib.cefswt_delete_cookies();
        };
        WebBrowser.NativeSetCookie = () -> {
            List<HttpCookie> cookies = HttpCookie.parse(WebBrowser.CookieValue);
            for (HttpCookie cookie : cookies) {
                long age = cookie.getMaxAge();
                if (age != -1) {
                    age = Instant.now().plusSeconds(age).getEpochSecond();
                }
                WebBrowser.CookieResult = lib.cefswt_set_cookie(WebBrowser.CookieUrl, 
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
            boolean result = lib.cefswt_get_cookie(WebBrowser.CookieUrl, cookieVisitor);
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
        cookieVisitor.visit.set((self, cefcookie, count, total, delete) -> {
            String name = lib.cefswt_cefstring_to_java(cefcookie.name);
            debug("Visitor " + count + "/" +total + ": " + name + ":" + Thread.currentThread());
            if (WebBrowser.CookieName != null && WebBrowser.CookieName.equals(name)) {
                String value = lib.cefswt_cookie_value(cefcookie);
//                debug("cookie value: " + value);
                WebBrowser.CookieValue = value;
                cookieVisited.complete(true);
                return 0;
            }
            return 1;
        });
    }

    private static void fixJNRClosureClassLoader() {
        try {
            ClosureManager closureManager = NativeRuntime.getInstance().getClosureManager();
            Field classLoader = findField(closureManager.getClass(), "classLoader");
            classLoader.setAccessible(true);
            Object asmClassLoader = classLoader.get(closureManager);

            Field parent = findField(ClassLoader.class, "parent");
            parent.setAccessible(true);
            parent.set(asmClassLoader, Chromium.class.getClassLoader());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Field findField(Class<?> class1, String name) throws Exception {
        return class1.getDeclaredField(name);
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
            lib.cefswt_go_back(browser);
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
        	lib.cefswt_execute(browser, script);
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
    	if (browser == null) {
        	if (paintListener != null) {
        		chromium.removePaintListener(paintListener);
        		paintListener = null;
        		createBrowser();
        	}
    	}
        Object[] ret = new Object[1];
        EvalReturned callback = (loop, type, value) -> {
        	//debugPrint("eval retured: " +type + ":"+value.length()+":"+value);
        	if (loop == 1) {
        		if (!(loopDisable && ("cocoa".equals(SWT.getPlatform()) || "gtk".equals(SWT.getPlatform())))) {
        			chromium.getDisplay().readAndDispatch();
        		}
        		if (!loopDisable) {
//        			lib.cefswt_do_message_loop_work();
        		}
        	} else {
        		ret[0] = mapType(type, value);
        	}
        };
        StringBuilder buffer = new StringBuilder ("(function() {");
        buffer.append ("\n");
        buffer.append (script);
        buffer.append ("\n})()");
        
        checkBrowser();
        boolean returnSt = lib.cefswt_eval(browser, buffer.toString(), EVAL++, callback);
        if (!returnSt) {
            throw new SWTException("Script that was evaluated failed");
        }
        return ret[0];
    }

    private Object mapType(ReturnType type, String value) throws SWTException {
        if (type == ReturnType.Error) {
              if ((SWT.ERROR_INVALID_RETURN_VALUE+"").equals(value)) {
                  throw new SWTException(SWT.ERROR_INVALID_RETURN_VALUE);
              }
              throw new SWTException(SWT.ERROR_FAILED_EVALUATE, value);
          } 
          else if (type == ReturnType.Null) {
              return null;
          } 
          else if (type == ReturnType.Bool) {
              return "1".equals(value) ? Boolean.TRUE : Boolean.FALSE ;
          } 
          else if (type == ReturnType.Double) {
              return Double.parseDouble(value);
          } 
          else if (type == ReturnType.Array) {
        	  String value_unquoted = value.substring(1, value.length()-1);
        	  String[] elements = value_unquoted.split(";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
              Object[] array = new Object[elements.length];
              for (int i = 0; i < array.length; i++) {
            	  String elemUnquoted = elements[i].substring(1, elements[i].length()-1);
            	  String[] parts = elemUnquoted.split(",(?=(?:[^']*'[^']*')*[^']*$)", 2);
            	  ReturnType elemType = CEFFactory.ReturnType.from(parts[0]);
            	  Object elemValue = mapType(elemType, parts[1]);
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
            lib.cefswt_go_forward(browser);
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
        if (browser != null && textVisitor != null) {
            debugPrint("update text");
            textReady = new CompletableFuture<String>();
            lib.cefswt_get_text(browser, textVisitor);
        }
    }

    @Override
    public String getUrl() {
    	if (lib == null) {
            SWT.error(SWT.ERROR_FAILED_LOAD_LIBRARY);
        }
    	if (browser == null) {
            if (this.url == null) {
                return "about:blank";
            }
    		return getPlainUrl(this.url);
    	}
        String cefurl = lib.cefswt_get_url(browser);
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
        if (browser != null) {
        	lib.cefswt_reload(browser);
        }
    }

    @Override
    public boolean setText(String html, boolean trusted) {
        String texturl = DATA_TEXT_URL + Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.ISO_8859_1));
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
        if (!chromium.isDisposed() && browser != null) {
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

	private void doSetUrlPost(Pointer browser, String url, String postData, String[] headers) {
		byte[] bytes = (postData != null) ? postData.getBytes(Charset.forName("ASCII")) : null;
		int bytesLength = (postData != null) ? bytes.length : 0 ;
		int headersLength = (headers != null) ? headers.length : 0 ; 
		lib.cefswt_load_url(browser, url, bytes, bytesLength, headers, headersLength);
	}
    
    @Override
    public void stop() {
    	if (lib == null) {
            SWT.error(SWT.ERROR_FAILED_LOAD_LIBRARY);
        }
    	if (browser != null) {
    		lib.cefswt_stop(browser);
    	}
    }

    public static interface Lib {
        void cefswt_init(@Direct CEF.cef_app_t app, String cefrustPath, String version, int debugPort);

        void cefswt_set_window_info_parent(Pointer windowInfo, Pointer client, @Direct CEF.cef_client_t clientHandler, long handle, int x, int y, int w, int h);

        Pointer cefswt_create_browser(long hwnd, String url, @Direct CEF.cef_client_t clientHandler, int w, int h, int js, int cefBgColor);

        boolean cefswt_is_same(@Direct Pointer browser, @Direct Pointer that);

        int cefswt_do_message_loop_work();

        void cefswt_load_url(Pointer browser, @Encoding("UTF8") String url, byte[] bytes, int length, String[] headers, int length2);

        void cefswt_load_text(Pointer browser, @Encoding("UTF8") String text);

        void cefswt_stop(Pointer browser);

        void cefswt_reload(Pointer browser);

        @Encoding("UTF8") String cefswt_get_url(Pointer browser);

        @Encoding("UTF8") String cefswt_get_text(Pointer browser, CEF.cef_string_visitor_t visitor);

        void cefswt_resized(Pointer browser, int width, int height);

        void cefswt_set_focus(Pointer browser, boolean focus, long shell_hwnd);

        void cefswt_go_forward(Pointer browser);

        void cefswt_go_back(Pointer browser);

        void cefswt_execute(Pointer browser, @Encoding("UTF8") String script);
        
        boolean cefswt_eval(Pointer browser, @Encoding("UTF8") String script, int id, EvalReturned callback);
        
        boolean cefswt_function(Pointer browser, @Encoding("UTF8") String name, int id);

        FunctionSt cefswt_function_id(Pointer msg);
        
        boolean cefswt_function_arg(Pointer msg, int index, EvalReturned callback);

        boolean cefswt_function_return(Pointer browser, int id, int port, ReturnType returnType, @Encoding("UTF8") String ret);
        
        void cefswt_close_browser(Pointer browser);
        
        boolean cefswt_is_main_frame(Pointer frame);

        void cefswt_shutdown();

        void cefswt_free(@Direct Pointer bs);

        @Encoding("UTF8") String cefswt_cefstring_to_java(CEF.cef_string_t string);

        @Encoding("UTF8") String cefswt_request_to_java(Pointer request);

        void cefswt_dialog_close(Pointer callback, int i, CEF.cef_string_t default_prompt_text);

        void cefswt_context_menu_cancel(Pointer callback);

        boolean cefswt_set_cookie(@Encoding("UTF8") String url, @Encoding("UTF8") String name, @Encoding("UTF8") String value, @Encoding("UTF8") String domain, @Encoding("UTF8") String path, int secure, int httpOnly, double maxAge);

        boolean cefswt_get_cookie(String url, CEF.cef_cookie_visitor_t visitor);

        void cefswt_delete_cookies();

        @Encoding("UTF8") String cefswt_cookie_value(CEF.cef_cookie_t cookie);
    }
}
