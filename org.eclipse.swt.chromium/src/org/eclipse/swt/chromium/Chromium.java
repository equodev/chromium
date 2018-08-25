package org.eclipse.swt.chromium;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.Direct;
import jnr.ffi.provider.ClosureManager;
import jnr.ffi.provider.jffi.NativeRuntime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.browser.VisibilityWindowListener;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.Library;
import org.eclipse.swt.internal.chromium.CEF;
import org.eclipse.swt.internal.chromium.CEFFactory;
import org.eclipse.swt.internal.chromium.ResourceExpander;
import org.eclipse.swt.internal.chromium.CEF.cef_client_t;

class Chromium extends WebBrowser {
    private static final String VERSION = "0300";
    private static final String CEFVERSION = "3071";
    private static final String SHARED_LIB_V = "chromium_swt-"+VERSION;
    private static final int MAX_PROGRESS = 100;
//    private static final String SUBP = "chromium_subp";
//    private static final String SUBP_V = "chromium_subp-"+VERSION;
    
    Browser chromium;
    OpenWindowListener[] openWindowListeners = new OpenWindowListener[0];
    public boolean jsEnabledOnNextPage = false;

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

    private long hwnd;
    private Pointer browser;
    private CEF.cef_client_t clientHandler;
    private CEF.cef_focus_handler_t focusHandler;
    private CEF.cef_life_span_handler_t lifeSpanHandler;
    private CEF.cef_load_handler_t loadHandler;
    private CEF.cef_display_handler_t displayHandler;
    private CEF.cef_string_visitor_t  textVisitor;
    private FocusListener focusListener;
    private String url;
    private String text = "";
    private boolean canGoBack;
    private boolean canGoForward;
    private boolean disposing;
    private WindowEvent popupWindowEvent;
    private boolean ignoreFirstEvents;
    private int instance;
    private static int INSTANCES = 0;

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
    }

    public void destroyFunction (BrowserFunction function) {
    }
    
    @Override
    public void create(Composite parent, int style) {
        initCEF(chromium.getDisplay());
//        chromium.addPaintListener(new PaintListener() {
//            @Override
//            public void paintControl(PaintEvent e) {
//                debugPrint("paintControl");
//                chromium.removePaintListener(this);
//                cefInitilized.thenRun(() -> { 
//                    debugPrint("cefInitilized Future CALLBACK");
//                    chromium.getDisplay().syncExec(() -> {
//                        try {
                            debugPrint("initCef Done");
                            createBrowser();
//                        } catch(Throwable e1) {
//                            browserInitilized.completeExceptionally(e1);
//                        }
//                    });
//                });
//                debugPrint("paintControl Done");
//            }
//        });
    }

    private jnr.ffi.Pointer debugPrint(String log) {
        System.out.println("J"+instance + ":" + log + (this.url != null ? " " + this.url : " empty url"));
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
                    if (browsers.get() > 0) lib.cefswt_do_message_loop_work();
                };
                browserProcessHandler.on_schedule_message_pump_work.set((pbrowserProcessHandler, delay) -> {
                    if (display.isDisposed())
                        return;
                    if (delay <= 0) {
                        display.asyncExec(runnable);
                    } else {
                        display.timerExec((int)delay, runnable);
                    }
                });
                
                app.get_browser_process_handler.set(appPtr -> {
//                    debugPrint("GetBrowserProcessHandler");
                    return browserProcessHandler;
                });
                System.out.println("cefrust.path: " + cefrustPath);
                lib.cefswt_init(app, cefrustPath, VERSION);
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
    
    private void createBrowser() {
        hwnd = getHandle(chromium);
        if (this.url == null) {
            this.url = "about:blank";
            ignoreFirstEvents = true;
        }

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

        chromium.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                if (!chromium.isDisposed() && browser != null) {
                    if (chromium.getDisplay().getActiveShell() != chromium.getShell()) {
//                      System.err.println("Ignore do_message_loop_work due inactive shell");
                        return;
                    }
                    lib.cefswt_resized(browser, chromium.getSize().x, chromium.getSize().y);
                }
            }
        });
        
        final org.eclipse.swt.graphics.Point size = chromium.getSize();
        browser = lib.cefswt_create_browser(hwnd, url, clientHandler, size.x, size.y);
        if (browser != null) {
            browsers.incrementAndGet();
            lib.cefswt_resized(browser, chromium.getSize().x, chromium.getSize().y);
        }

        final Display display = chromium.getDisplay();
        if (browsers.get() == 1) {
            debugPrint("STARTING MSG LOOP");
            doMessageLoop(display);
        }
    }

    private void set_life_span_handler() {
        lifeSpanHandler = CEFFactory.newLifeSpanHandler();
        lifeSpanHandler.on_before_close.set((plifeSpanHandler, browser) -> {
            debugPrint("OnBeforeClose");
            lib.cefswt_free(browser);
            Chromium.this.clientHandler = null;
            Chromium.this.browser = null;
            Chromium.this.focusHandler = null;
            Chromium.this.lifeSpanHandler = null;
            Chromium.this.browser = null;
            // not always called on linux
            String platform = SWT.getPlatform();
            if (("win32".equals(platform) || "cocoa".equals(platform)) && browsers.decrementAndGet() == 0 && shuttindDown) {
                internalShutdown();
            }
        });
        lifeSpanHandler.do_close.set((plifeSpanHandler, browser) -> {
            //lifeSpanHandler.base.ref++;
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
            
            
            String platform = SWT.getPlatform();
            if (disposing && ("gtk".equals(platform)) && browsers.decrementAndGet() == 0 && shuttindDown) {
                internalShutdown();
            }
            if (!disposing) {
                chromium.dispose();
            }
            // do not send close notification to top level window
            // returning 0, cause the window to close 
            return 1;
        });
        lifeSpanHandler.on_after_created.set((self, browser) -> {
            if (chromium.isDisposed() || visibilityWindowListeners == null) return;
            debugPrint("on_after_created " + browser + ":" + popupWindowEvent);
            if (Chromium.this.browser != null && !browser.equals(Chromium.this.browser)) {
                // replacing this browser with popup browser. TODO: destroy original
                Chromium.this.browser = browser;
            }
            chromium.getDisplay().asyncExec(() -> {
                debugPrint("on_after_created handling " + browser + ":" + popupWindowEvent);
                if (chromium == null || chromium.isDisposed() || visibilityWindowListeners == null) return;
                org.eclipse.swt.browser.WindowEvent event = new org.eclipse.swt.browser.WindowEvent(chromium);
                event.display = chromium.getDisplay ();
                event.widget = chromium;
                event.size = new Point(0,0);
                event.location = new Point(0,0);
                if (popupWindowEvent != null) {
                    event.size = popupWindowEvent.size;
                    event.location = popupWindowEvent.location;
                    event.addressBar = popupWindowEvent.addressBar;
                    event.menuBar = popupWindowEvent.menuBar;
                    event.statusBar = popupWindowEvent.statusBar;
                    event.toolBar = popupWindowEvent.toolBar;
                    popupWindowEvent = null;
                }
                for (VisibilityWindowListener listener : visibilityWindowListeners) {
                    listener.show(event);
                }
            });
        });
        lifeSpanHandler.on_before_popup.set((self, browser, frame,
                target_url, target_frame_name, target_disposition,
                user_gesture, popupFeatures, windowInfo,
                client, settings, no_javascript_access) -> {
            debugPrint("on_before_popup " + browser + ":" + popupWindowEvent);
            if (chromium.isDisposed()) 
                return 1;
            if (openWindowListeners == null) 
                return 0;
            
            WindowEvent event = new WindowEvent(chromium);
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
            chromium.getDisplay().syncExec(() -> {
                for (OpenWindowListener listener : openWindowListeners) {
                    listener.open(event);
                }
            });
            if (event.browser == null && event.required)
                return 1;
            if (event.browser != null) {
                event.browser.webBrowser.popupWindowEvent = event;
                chromium.getDisplay().syncExec(() -> {
                    lib.cefswt_set_window_info_parent(windowInfo, client, event.browser.webBrowser.clientHandler, event.browser.webBrowser.getHandle(event.browser));
                });
            }
            return 0;
        });
        clientHandler.get_life_span_handler.set(client -> {
            //DEBUG_CALLBACK("GetLifeSpanHandler");
            return lifeSpanHandler;
        });
    }
    
    private void set_load_handler() {
        loadHandler = CEFFactory.newLoadHandler();
        loadHandler.on_loading_state_change.set((self_, browser, isLoading, canGoBack, canGoForward) -> {
            //debugPrint("on_loading_state_change " + isLoading);
            Chromium.this.canGoBack = canGoBack == 1;
            Chromium.this.canGoForward = canGoForward == 1;
            if (chromium.isDisposed() || progressListeners == null) return;
            updateText();
            if (!("about:blank".equals(url) && ignoreFirstEvents)) {
                ProgressEvent event = new ProgressEvent(chromium);
                event.display = chromium.getDisplay ();
                event.widget = chromium;
                event.current = MAX_PROGRESS;
                event.current = isLoading == 1 ? 1 : MAX_PROGRESS;
                event.total = MAX_PROGRESS;
                for (ProgressListener listener : progressListeners) {
                    if (isLoading == 1) {
                        listener.changed(event);
                    } else {
                        listener.completed(event);
                    }
                }
            } else if (isLoading == 0 && ignoreFirstEvents) {
                ignoreFirstEvents = false;
            }
        });
        clientHandler.get_load_handler.set(client -> {
            return loadHandler;
        });
    }

    private void set_display_handler() {
        displayHandler = CEFFactory.newDisplayHandler();
        displayHandler.on_title_change.set((self, browser, title) -> {
            if (chromium.isDisposed() || progressListeners == null) return;
            String str = lib.cefswt_cefstring_to_java(title);
            TitleEvent event = new TitleEvent(chromium);
            event.display = chromium.getDisplay ();
            event.widget = chromium;
            event.title = str;
            for (TitleListener listener : titleListeners) {
                listener.changed(event);
            }
        });
        displayHandler.on_address_change.set((self, browser, frame, url) -> {
            if (chromium.isDisposed() || locationListeners == null) return;
            LocationEvent event = new LocationEvent(chromium);
            event.display = chromium.getDisplay();
            event.widget = chromium;
            event.doit = true;
            event.location = lib.cefswt_cefstring_to_java(url);
            event.top = lib.cefswt_is_main_frame(frame);
            debugPrint("on_address_change:" + event.location);
            if (!("about:blank".equals(event.location) && ignoreFirstEvents)) {
                for (LocationListener listener : locationListeners) {
                    listener.changed(event);
                }
            }
        });
        clientHandler.get_display_handler.set(client -> {
            return displayHandler;
        });
    }
    
    private void set_text_visitor() {
        textVisitor = CEFFactory.newStringVisitor();
        textVisitor.visit.set((self, cefString) -> {
            String newtext = lib.cefswt_cefstring_to_java(cefString);
            if (newtext != null) {
                text = newtext;
            }
        });
    }

    private void set_focus_handler() {
        focusHandler = CEFFactory.newFocusHandler();
        focusHandler.on_got_focus.set((focusHandler, browser_1) -> {
            debugPrint("CALLBACK OnGotFocus");
            if (!isFocusControl()) {
                chromium.removeFocusListener(focusListener);
                boolean r = chromium.forceFocus();
                debugPrint("Forcing focus to SWT canvas: " + r);
                if (r) {
                    browserFocus(true);
                }
                chromium.addFocusListener(focusListener);
            }
        });
        focusHandler.on_set_focus.set((focusHandler, browser_1, focusSource) -> {
            debugPrint("CALLBACK OnSetFocus " + focusSource);
            if (!isFocusControl()) {
                debugPrint("Disallowing focus to SWT canvas");
                chromium.removeFocusListener(focusListener);
                chromium.setFocus();
                chromium.addFocusListener(focusListener);
                return 1;
            }
            //System.out.println("Allowing focus to SWT canvas");
            return 0;
        });
        focusHandler.on_take_focus.set((focusHandler, browser_1, next) -> {
            debugPrint("CALLBACK OnTakeFocus " + next);
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
            debugPrint("GetFocusHandler");
            return focusHandler;
        });
    }
    
    // single loop for all browsers
    private static void doMessageLoop(final Display display) {
        final int loop = 5;
        display.timerExec(loop, new Runnable() {
            public void run() {
                if (lib != null && browsers.get() > 0) {
                    lib.cefswt_do_message_loop_work();
                    display.timerExec(loop, this);
                } else {
                    debug("STOPPING MSG LOOP");
                }
            }
        });
    }
    
    private synchronized void checkBrowser() {
        if (lib == null) {
            SWT.error(SWT.ERROR_FAILED_LOAD_LIBRARY);
        }
        if (browser == null) {
            SWT.error(SWT.ERROR_WIDGET_DISPOSED);
        }
    }

    protected static void initializeClientHandler(CEF.cef_client_t client) {
        // callbacks
        client.get_context_menu_handler.set((c) -> debug("get_context_menu_handler"));
        client.get_dialog_handler.set((c) -> debug("get_dialog_handler"));
        client.get_download_handler.set((c) -> debug("get_download_handler"));
        client.get_drag_handler.set((c) -> debug("get_drag_handler"));
        client.get_geolocation_handler.set((c) -> debug("get_geolocation_handler"));
        client.get_jsdialog_handler.set((c) -> debug("get_jsdialog_handler"));
        client.get_keyboard_handler.set((c) -> null);
        client.get_render_handler.set((c) -> null);
        client.get_request_handler.set((c) -> null);
        client.on_process_message_received.set((c, browser_1, processId_2, processMessage_3) -> {
            debug("on_process_message_received");
            return 0;
        });
        client.get_find_handler.set(c -> debug("setGetFindHandler"));
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
            if (gtk.startsWith("3")) {
                throw new SWTException(SWT.ERROR_FAILED_LOAD_LIBRARY, "Chromium Browser is not supported in GTK3 yet. "
                        + "Set env var SWT_GTK3=0");
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
            return libc;
        } catch(UnsatisfiedLinkError e) {
            String cefLib = System.mapLibraryName("cef");
            if ("cocoa".equals(platform)) {
            	cefLib = "Chromium Embedded Framework.framework";
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
        checkBrowser();
        if (canGoBack) {
            lib.cefswt_go_back(browser);
            return true;
        }
        return false;
    }

    @Override
    public boolean execute(String script) {
        lib.cefswt_execute(browser, script);
        return true;
    }

    @Override
    public boolean forward() {
        checkBrowser();
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
            lib.cefswt_get_text(browser, textVisitor);
        }
    }

    @Override
    public String getUrl() {
        checkBrowser();
        String cefurl = lib.cefswt_get_url(browser);
//        debugPrint("getUrl1:" + cefurl);
        if (cefurl == null)
            cefurl = this.url;
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
        // TODO Auto-generated method stub

    }

    @Override
    public boolean setText(String html, boolean trusted) {
        checkBrowser();
        text = html;
        lib.cefswt_load_text(browser, html);
        return true;
    }

    @Override
    public boolean setUrl(String url, String postData, String[] headers) {
        // if not yet created will be used when created
        if (!chromium.isDisposed() && browser != null) {
            debugPrint("setUrl: " + url);
            lib.cefswt_load_url(browser, url);
        }
        this.url = url;
        return true;
    }
    
    @Override
    public void stop() {
        checkBrowser();
        lib.cefswt_stop(browser);
    }

    public static interface Lib {
        void cefswt_init(@Direct CEF.cef_app_t app, String cefrustPath, String version);

        void cefswt_set_window_info_parent(@Direct Pointer windowInfo, @Direct Pointer client, @Direct cef_client_t clientHandler, long handle);

        Pointer cefswt_create_browser(long hwnd, String url, @Direct CEF.cef_client_t clientHandler, int w, int h);

        void cefswt_do_message_loop_work();

        void cefswt_load_url(Pointer browser, String url);

        void cefswt_load_text(Pointer browser, String text);

        void cefswt_stop(Pointer browser);

        String cefswt_get_url(Pointer browser);

        String cefswt_get_text(Pointer browser, CEF.cef_string_visitor_t visitor);

        void cefswt_resized(Pointer browser, int width, int height);

        void cefswt_set_focus(Pointer browser, boolean focus, long shell_hwnd);

        void cefswt_go_forward(Pointer browser);

        void cefswt_go_back(Pointer browser);

        void cefswt_execute(Pointer browser, String script);
        
        void cefswt_close_browser(Pointer browser);

        void cefswt_shutdown();

        void cefswt_free(@Direct Pointer bs);

        String cefswt_cefstring_to_java(CEF.cef_string_t string);

        boolean cefswt_is_main_frame(Pointer frame);
    }
}
