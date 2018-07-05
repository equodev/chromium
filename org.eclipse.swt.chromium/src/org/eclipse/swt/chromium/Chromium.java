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
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.internal.Library;
import org.eclipse.swt.internal.chromium.CEF;
import org.eclipse.swt.internal.chromium.CEFFactory;
import org.eclipse.swt.internal.chromium.ResourceExpander;

class Chromium extends WebBrowser {
    private static final String VERSION = "0300";
    private static final String CEFVERSION = "3071";
    private static final String SHARED_LIB_V = "chromium_swt-"+VERSION;
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
    private FocusListener focusListener;
    private String url;
    private boolean disposing;

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
        System.out.println("J:" + log + (this.url != null ? " " + this.url : " empty url"));
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
        }

        chromium.addDisposeListener(e -> {
            debugPrint("disposing chromium");
            dispose();
        });
        focusListener = new CefFocusListener();
        chromium.addFocusListener(focusListener);

        clientHandler = CEFFactory.newClient();
        initializeClientHandler(clientHandler);
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
            String platform = SWT.getPlatform();
            if (("gtk".equals(platform)) && browsers.decrementAndGet() == 0 && shuttindDown) {
                internalShutdown();
            }
            // do not send close notification to top level window
            // return 0, cause the window to close 
            return 1;
        });
        clientHandler.get_life_span_handler.set(client -> {
            //DEBUG_CALLBACK("GetLifeSpanHandler");
            return lifeSpanHandler;
        });

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
        client.get_display_handler.set((c) -> null);
        client.get_download_handler.set((c) -> debug("get_download_handler"));
        client.get_drag_handler.set((c) -> debug("get_drag_handler"));
        client.get_focus_handler.set((c) -> null);
        client.get_geolocation_handler.set((c) -> debug("get_geolocation_handler"));
        client.get_jsdialog_handler.set((c) -> debug("get_jsdialog_handler"));
        client.get_keyboard_handler.set((c) -> null);
        client.get_life_span_handler.set((c) -> null);
        client.get_load_handler.set((c) -> null);
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean execute(String script) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean forward() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getBrowserType() {
        return "chromium";
    }

    @Override
    public String getText() {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isForwardEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void refresh() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean setText(String html, boolean trusted) {
        // TODO Auto-generated method stub
        return false;
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
        // TODO Auto-generated method stub

    }

    public static interface Lib {
        void cefswt_init(@Direct CEF.cef_app_t app, String cefrustPath, String version);

        Pointer cefswt_create_browser(long hwnd, String url, @Direct CEF.cef_client_t clientHandler, int w, int h);

        void cefswt_do_message_loop_work();

        void cefswt_load_url(Pointer browser, String url);

        String cefswt_get_url(Pointer browser);
        
        void cefswt_resized(Pointer browser, int width, int height);

        void cefswt_set_focus(Pointer browser, boolean focus, long shell_hwnd);

        void cefswt_close_browser(Pointer browser);

        void cefswt_shutdown();
        
        void cefswt_free(@Direct Pointer bs);
    }
}
