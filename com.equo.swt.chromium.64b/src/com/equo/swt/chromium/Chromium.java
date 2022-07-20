/****************************************************************************
**
** Copyright (C) 2021 Equo
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

package com.equo.swt.chromium;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.C;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.internal.Library;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import com.equo.swt.internal.chromium.CefFactory;
import com.equo.swt.internal.chromium.CefFactory.ReturnType;
import com.equo.swt.internal.chromium.ResourceExpander;
import com.equo.swt.internal.chromium.lib.ChromiumLib;
import com.equo.swt.internal.chromium.lib.FunctionSt;
import com.equo.swt.internal.chromium.lib.cef_app_t;
import com.equo.swt.internal.chromium.lib.cef_browser_process_handler_t;
import com.equo.swt.internal.chromium.lib.cef_client_t;
import com.equo.swt.internal.chromium.lib.cef_context_menu_handler_t;
import com.equo.swt.internal.chromium.lib.cef_cookie_visitor_t;
import com.equo.swt.internal.chromium.lib.cef_display_handler_t;
import com.equo.swt.internal.chromium.lib.cef_focus_handler_t;
import com.equo.swt.internal.chromium.lib.cef_jsdialog_handler_t;
import com.equo.swt.internal.chromium.lib.cef_key_event_t;
import com.equo.swt.internal.chromium.lib.cef_keyboard_handler_t;
import com.equo.swt.internal.chromium.lib.cef_life_span_handler_t;
import com.equo.swt.internal.chromium.lib.cef_load_handler_t;
import com.equo.swt.internal.chromium.lib.cef_popup_features_t;
import com.equo.swt.internal.chromium.lib.cef_request_handler_t;
import com.equo.swt.internal.chromium.lib.cef_string_visitor_t;

class Chromium extends WebBrowser {
  private static final String DATA_TEXT_URL = "data:text/html;base64,";
  private static final String VERSION = "6900";
  private static final String CEFVERSION = "3497";
  private static final String SHARED_LIB_V = "chromium_swt_" + VERSION;
  private static final String JNI_LIB_V = "swt-chromium-" + VERSION;
  private static final int MAX_PROGRESS = 100;
  private static final int LOOP = 75;
  private static final boolean debug =
      Boolean.valueOf(System.getProperty("swt.chromium.debug", "false"));

  static {
    lib = loadLib();
  }

  private static Object lib;
  private static String cefrustPath;
  //    private static CompletableFuture<Boolean> cefInitilized;
  private static cef_app_t app;
  private static cef_browser_process_handler_t browserProcessHandler;
  private static boolean shuttindDown;
  private static cef_cookie_visitor_t cookieVisitor;
  private static CompletableFuture<Boolean> cookieVisited;
  private static AtomicInteger browsers = new AtomicInteger(0);
  private static Map<Integer, Chromium> instances = new HashMap<>();
  private static int EVAL = 1;
  private static int INSTANCES = 0;
  private static Runnable loopWork;
  private static boolean loopDisable;
  private static boolean pumpDisable;
  private static int disposingAny = 0;
  private static int popupHandlers = 0;

  private static cef_client_t clientHandler;
  private static cef_focus_handler_t focusHandler;
  private static cef_keyboard_handler_t keyboardHandler;
  private static cef_life_span_handler_t lifeSpanHandler;
  private static cef_load_handler_t loadHandler;
  private static cef_display_handler_t displayHandler;
  private static cef_request_handler_t requestHandler;
  private static cef_jsdialog_handler_t jsDialogHandler;
  private static cef_context_menu_handler_t contextMenuHandler;
  private static cef_client_t popupClientHandler;
  private static cef_life_span_handler_t popupLifeSpanHandler;

  private cef_string_visitor_t textVisitor;
  Browser chromium;
  OpenWindowListener[] openWindowListeners = new OpenWindowListener[0];
  private long /* int */ hwnd;
  private long /* int */ browser;
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
  private int disposing;
  private int instance;
  private boolean hasFocus;
  private boolean ignoreFirstFocus = true;
  private PaintListener paintListener;
  private Listener traverseListener;
  private WindowEvent isPopup;

  public Chromium() {
  }

  public void addOpenWindowListener(OpenWindowListener listener) {
    OpenWindowListener[] newOpenWindowListeners =
        new OpenWindowListener[openWindowListeners.length + 1];
    System.arraycopy(openWindowListeners, 0, newOpenWindowListeners, 0, openWindowListeners.length);
    openWindowListeners = newOpenWindowListeners;
    openWindowListeners[openWindowListeners.length - 1] = listener;
  }

  public void removeOpenWindowListener(OpenWindowListener listener) {
    if (openWindowListeners.length == 0) {
      return;
    }
    int index = -1;
    for (int i = 0; i < openWindowListeners.length; i++) {
      if (listener == openWindowListeners[i]) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      return;
    }
    if (openWindowListeners.length == 1) {
      openWindowListeners = new OpenWindowListener[0];
      return;
    }
    OpenWindowListener[] newOpenWindowListeners =
        new OpenWindowListener[openWindowListeners.length - 1];
    System.arraycopy(openWindowListeners, 0, newOpenWindowListeners, 0, index);
    System.arraycopy(openWindowListeners, index + 1, newOpenWindowListeners, index,
        openWindowListeners.length - index - 1);
    openWindowListeners = newOpenWindowListeners;
  }

  public void setBrowser(Browser browser) {
    super.setBrowser(browser);
    this.chromium = browser;
  }

  @Override
  public void createFunction(BrowserFunction function) {
    created.thenRun(() -> {
      checkBrowser();

      for (BrowserFunction current : functions.values()) {
        if (current.name.equals(function.name)) {
          deregisterFunction(current);
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

  public void destroyFunction(BrowserFunction function) {
    checkBrowser();
    deregisterFunction(function);
  }

  @Override
  public void create(Composite parent, int style) {
    initCef(chromium.getDisplay());
    //        debugPrint("initCef Done");
    chromium.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
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

  private void debugPrint(String log) {
    if (debug) {
      System.out.println("J" + instance + ":" + Thread.currentThread().getName() + ":" + log
          + (this.url != null ? " (" + getPlainUrl(this.url) + ")" : " empty-url"));
    }
  }

  private static void debug(String log) {
    if (debug) {
      System.out.println("J:" + log);
    }
  }

  private void initCef(Display display) {
    synchronized (lib) {
      if (app == null) {
        //                CEFFactory.create();
        app = CefFactory.newApp();
        //                cefInitilized = new CompletableFuture<>();
        browserProcessHandler = CefFactory.newBrowserProcessHandler();
        //    browserProcessHandler.on_context_initialized_cb = new Callback(
        //        Chromium.class, "on_context_initialized", void.class, new Type[] {long.class});
        //    browserProcessHandler.on_context_initialized = 
        //        browserProcessHandler.on_context_initialized_cb.getAddress();
        browserProcessHandler.on_schedule_message_pump_work_cb =
            new Callback(Chromium.class, "on_schedule_message_pump_work", void.class,
                new Type[] { long.class, int.class, int.class });
        browserProcessHandler.on_schedule_message_pump_work =
            checkGetAddress(browserProcessHandler.on_schedule_message_pump_work_cb);
        //              
        app.get_browser_process_handler_cb = new Callback(Chromium.class,
            "get_browser_process_handler", long.class, new Type[] { long.class });
        app.get_browser_process_handler = checkGetAddress(app.get_browser_process_handler_cb);

        browserProcessHandler.ptr = C.malloc(cef_browser_process_handler_t.sizeof);
        ChromiumLib.memmove(browserProcessHandler.ptr, browserProcessHandler,
            cef_browser_process_handler_t.sizeof);

        int debugPort = 0;
        try {
          debugPort = Integer
              .parseInt(System.getProperty("org.eclipse.swt.chromium.remote-debugging-port", "0"));
        } catch (NumberFormatException e) {
          debugPort = 0;
        }
        app.ptr = C.malloc(cef_app_t.sizeof);
        ChromiumLib.memmove(app.ptr, app, cef_app_t.sizeof);
        ChromiumLib.cefswt_init(app.ptr, cefrustPath, VERSION, debugPort);

        display.disposeExec(() -> {
          if (app == null || shuttindDown) {
            // already shutdown
            return;
          }
          internalShutdown();
        });
      }
    }
  }

  static long /* int */ get_browser_process_handler(long /* int */ app) {
    //        debug("GetBrowserProcessHandler");
    if (browserProcessHandler == null) {
      return 0;
    }
    return browserProcessHandler.ptr;
  }

  //    static void on_context_initialized(long /*int*/ browserProcessHandler) {
  //        debug("OnContextInitialized");
  //    }

  static Runnable loopWorkRunnable = () -> {
    Display display = Display.getCurrent();
    if (display == null || display.isDisposed() /* || display.getActiveShell() != getShell() */) {
      //System.err.println("Ignore do_message_loop_work due inactive shell");
      return;
    }
    //        debug("WORK PUMP");
    safe_loop_work("pump");
  };

  static void on_schedule_message_pump_work(long /* int */ pbrowserProcessHandler, int delay,
      int _delay2) {
    if (browsers.get() <= 0 || pumpDisable || disposingAny > 0) {
      return;
    }
    Display display = Display.getDefault();
    //        debug("pump "+delay);
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

  private static void safe_loop_work(String from) {
    if (browsers.get() > 0 && !loopDisable) {
      //          debug("safe_loop_work "+from);
      if (ChromiumLib.cefswt_do_message_loop_work() == 0) {
        System.err.println("error looping chromium");
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

  private long /* int */ getHandle(Composite control) {
    long /* int */ hwnd = 0;
    String platform = SWT.getPlatform();
    if ("cocoa".equals(platform)) {
      try {
        Field field = Control.class.getDeclaredField("view");
        field.setAccessible(true);
        Object nsview = field.get(control);

        Class<?> idClass = Class.forName("org.eclipse.swt.internal.cocoa.id");
        Field idField = idClass.getField("id");

        hwnd = (long /* int */) idField.get(nsview);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if ("win32".equals(platform)) {
      try {
        Field field = Control.class.getDeclaredField("handle");
        field.setAccessible(true);
        hwnd = (long /* int */) field.get(control);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      try {
        Field field = Widget.class.getDeclaredField("handle");
        field.setAccessible(true);
        hwnd = (long /* int */) field.get(control);
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

    //        set_text_visitor();
    //        if (browsers.get() == 0) {
    if (INSTANCES == 0) {
      set_client_handler();
    }

    chromium.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        if (!isDisposed() && browser != 0) {
          Point size = getChromiumSize();
          ChromiumLib.cefswt_resized(browser, size.x, size.y);
        }
      }
    });
  }

  private void createBrowser() {
    if (this.url == null) {
      this.url = "about:blank";
    }
    prepareBrowser();
    if (!"gtk".equals(SWT.getPlatform())) {
      traverseListener = new Listener() {
        @Override
        public void handleEvent(Event event) {
          if (event.type == SWT.Traverse) {
            //debugPrint("Ignoring TRAVERSE " + event);
            event.doit = false;
          }
        }
      };
    }
    if ("win32".equals(SWT.getPlatform())) {
      chromium.addListener(SWT.Traverse, traverseListener);
      chromium.addListener(SWT.KeyDown, traverseListener); // required to take focus on tab
    }
    if ("cocoa".equals(SWT.getPlatform())) {
      chromium.addListener(SWT.KeyDown, traverseListener); // required to take focus on tab
    }
    final Display display = chromium.getDisplay();
    final Color bg = chromium.getBackground();
    final Color bgColor = bg != null ? bg : display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
    final int cefBgColor =
        cefColor(bgColor.getAlpha(), bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue());

    final org.eclipse.swt.graphics.Point size = getChromiumSize();

    instance = ++INSTANCES;
    debug("Registering chromium instance " + instance);
    instances.put(instance, this);
    ChromiumLib.cefswt_create_browser(hwnd, url, clientHandler.ptr, size.x, size.y,
        jsEnabledOnNextPage ? 1 : 0, cefBgColor);
  }

  private void createPopup(long /* int */ windowInfo, long /* int */ client, WindowEvent event) {
    if (paintListener != null) {
      chromium.removePaintListener(paintListener);
      paintListener = null;
    } else {
      // TODO: destroy browser first?
      //instances.put(instance, this);
      debug("Unregistering chromium phantom popup " + instance);
      instances.remove(instance);
    }
    instance = ++INSTANCES;
    debug("Registering chromium popup " + instance);
    instances.put(instance, this);
    isPopup = event;

    String platform = SWT.getPlatform();
    if ("gtk".equals(platform) && chromium.getDisplay().getActiveShell() != chromium.getShell()) {
      // on linux the window hosting the popup needs to be created
      boolean visible = chromium.getShell().isVisible();
      chromium.getShell().open();
      chromium.getShell().setVisible(visible);
    }

    prepareBrowser();
    long /* int */ popupHandle = hwnd;
    debugPrint("popup will use hwnd:" + popupHandle);
    Point size = new Point(0, 0);
    if ("gtk".equals(SWT.getPlatform())) {
      size = chromium.getParent().getSize();
      size = DPIUtil.autoScaleUp(size);
    }

    ChromiumLib.cefswt_set_window_info_parent(windowInfo, client, clientHandler.ptr, popupHandle, 0,
        0, size.x, size.y);
    debugPrint("reparent popup");
  }

  private void createDefaultPopup(long /* int */ windowInfo, long /* int */ client,
      WindowEvent event) {
    debugPrint("default popup");
    instance = ++INSTANCES;
    debug("Registering chromium default popup " + instance);

    if (popupHandlers == 0) {
      popupLifeSpanHandler = CefFactory.newLifeSpanHandler();
      popupLifeSpanHandler.on_after_created_cb = new Callback(Chromium.class,
          "popup_on_after_created", void.class, new Type[] { long.class, long.class });
      popupLifeSpanHandler.on_after_created =
          checkGetAddress(popupLifeSpanHandler.on_after_created_cb);
      popupLifeSpanHandler.on_before_close_cb = new Callback(Chromium.class,
          "popup_on_before_close", void.class, new Type[] { long.class, long.class });
      popupLifeSpanHandler.on_before_close =
          checkGetAddress(popupLifeSpanHandler.on_before_close_cb);
      popupLifeSpanHandler.do_close_cb = new Callback(Chromium.class, "popup_do_close", int.class,
          new Type[] { long.class, long.class });
      popupLifeSpanHandler.do_close = checkGetAddress(popupLifeSpanHandler.do_close_cb);

      popupLifeSpanHandler.ptr = C.malloc(cef_life_span_handler_t.sizeof);
      ChromiumLib.memmove(popupLifeSpanHandler.ptr, popupLifeSpanHandler,
          cef_life_span_handler_t.sizeof);

      if (popupClientHandler == null) {
        popupClientHandler = CefFactory.newClient();
        popupClientHandler.get_life_span_handler_cb = new Callback(Chromium.class,
            "popup_get_life_span_handler", long.class, new Type[] { long.class });
        popupClientHandler.get_life_span_handler =
            checkGetAddress(popupClientHandler.get_life_span_handler_cb);

        popupClientHandler.ptr = C.malloc(cef_client_t.sizeof);
        ChromiumLib.memmove(popupClientHandler.ptr, popupClientHandler, cef_client_t.sizeof);
      }
    }
    popupHandlers++;

    ChromiumLib.cefswt_set_window_info_parent(windowInfo, client, popupClientHandler.ptr, 0,
        event.location != null ? event.location.x : 0,
        event.location != null ? event.location.y : 0, event.size != null ? event.size.x : 0,
        event.size != null ? event.size.y : 0);
  }

  static long /* int */ popup_get_life_span_handler(long /* int */ client) {
    if (popupLifeSpanHandler == null) {
      return 0;
    }
    return popupLifeSpanHandler.ptr;
  }

  static void popup_on_after_created(long /* int */ plifeSpanHandler, long /* int */ browser) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("popup on_after_created: " + id);
    try {
      // not sleeping here causes deadlock with multiple window.open
      Thread.sleep(LOOP);
    } catch (InterruptedException e) {
      // Undefined behavior
    }
  }

  static void popup_on_before_close(long /* int */ plifeSpanHandler, long /* int */ browser) {
    debug("popup OnBeforeClose");
    popupHandlers--;
    if (popupHandlers == 0) {
      disposeCallback(popupLifeSpanHandler.on_after_created_cb);
      disposeCallback(popupLifeSpanHandler.do_close_cb);
      disposeCallback(popupLifeSpanHandler.on_before_close_cb);
      C.free(popupLifeSpanHandler.ptr);
      popupLifeSpanHandler = null;
    }
    disposingAny--;
  }

  static int popup_do_close(long /* int */ plifeSpanHandler, long /* int */ browser) {
    debug("popup DoClose");
    disposingAny++;
    return 0;
  }

  private int cefColor(int a, int r, int g, int b) {
    return (a << 24) | (r << 16) | (g << 8) | (b << 0);
  }

  private Point getChromiumSize() {
    Point size = chromium.getSize();
    if ("cocoa".equals(SWT.getPlatform())) {
      return size;
    }
    return DPIUtil.autoScaleUp(size);
  }

  private static void set_client_handler() {
    clientHandler = CefFactory.newClient();
    set_focus_handler();
    set_keyboard_handler();
    set_life_span_handler();
    set_load_handler();
    set_display_handler();
    set_request_handler();
    set_jsdialog_handler();
    set_context_menu_handler();
    clientHandler.on_process_message_received_cb =
        new Callback(Chromium.class, "on_process_message_received", int.class,
            new Type[] { long.class, long.class, int.class, long.class });
    clientHandler.on_process_message_received =
        checkGetAddress(clientHandler.on_process_message_received_cb);

    clientHandler.ptr = C.malloc(cef_client_t.sizeof);
    ChromiumLib.memmove(clientHandler.ptr, clientHandler, cef_client_t.sizeof);
  }

  private static void set_life_span_handler() {
    lifeSpanHandler = CefFactory.newLifeSpanHandler();
    lifeSpanHandler.on_before_close_cb = new Callback(Chromium.class, "on_before_close", void.class,
        new Type[] { long.class, long.class });
    lifeSpanHandler.on_before_close = checkGetAddress(lifeSpanHandler.on_before_close_cb);
    lifeSpanHandler.do_close_cb =
        new Callback(Chromium.class, "do_close", int.class, new Type[] { long.class, long.class });
    lifeSpanHandler.do_close = checkGetAddress(lifeSpanHandler.do_close_cb);
    lifeSpanHandler.on_after_created_cb = new Callback(Chromium.class, "on_after_created",
        void.class, new Type[] { long.class, long.class });
    lifeSpanHandler.on_after_created = checkGetAddress(lifeSpanHandler.on_after_created_cb);
    lifeSpanHandler.on_before_popup_cb = new Callback(Chromium.class, "on_before_popup", int.class,
        new Type[] { long.class, long.class, long.class, long.class, long.class, int.class,
            int.class, long.class, long.class, long.class, long.class, int.class });
    lifeSpanHandler.on_before_popup = checkGetAddress(lifeSpanHandler.on_before_popup_cb);

    clientHandler.get_life_span_handler_cb = new Callback(Chromium.class, "get_life_span_handler",
        long.class, new Type[] { long.class });
    clientHandler.get_life_span_handler = checkGetAddress(clientHandler.get_life_span_handler_cb);
    lifeSpanHandler.ptr = C.malloc(cef_life_span_handler_t.sizeof);
    ChromiumLib.memmove(lifeSpanHandler.ptr, lifeSpanHandler, cef_life_span_handler_t.sizeof);
  }

  static long /* int */ get_life_span_handler(long /* int */ client) {
    //        debug("GetLifeSpanHandler");
    if (lifeSpanHandler == null) {
      return 0;
    }
    return lifeSpanHandler.ptr;
  }

  static void on_before_close(long /* int */ plifeSpanHandler, long /* int */ browser) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("OnBeforeClose" + id);
    instances.remove(id).on_before_close(browser);

    int decrementAndGet = browsers.decrementAndGet();

    ChromiumLib.cefswt_free(browser);
    if (decrementAndGet == 0) {
      //      debug("freelAll now");
      //      freeAll(display);
    }
    // not always called on linux
    disposingAny--;
    if (decrementAndGet == 0 && shuttindDown) {
      internalShutdown();
    }
  }

  private void on_before_close(long /* int */ browser) {
    this.browser = 0;
    this.chromium = null;
    debugPrint("closed");
    if (textVisitor != null) {
      //          debugPrint("text visitor still pending");
      Display.getCurrent().asyncExec(() -> {
        if (textVisitor != null) {
          freeTextVisitor();
        }
      });
    }
  }

  private static synchronized void freeAll(Display display) {
    if (!instances.isEmpty()) {
      System.err.println("freeing all handlers, but there are instances");
    }
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
      disposeCallback(get_life_span_handler_cb);
      disposeCallback(get_request_handler_cb);
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
    if (keyboardHandler != null) {
      C.free(keyboardHandler.ptr);
      disposeCallback(keyboardHandler.on_key_event_cb);
      keyboardHandler = null;
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
      disposeCallback(requestHandler.get_auth_credentials_cb);
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
    if (popupClientHandler != null) {
      disposeCallback(popupClientHandler.get_life_span_handler_cb);
      C.free(popupClientHandler.ptr);
      popupClientHandler = null;
    }

    debug("all dipsosed");
  }

  static int do_close(long /* int */ plifeSpanHandler, long /* int */ browser) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("DoClose: " + id);
    return safeGeInstance(id).do_close(browser);
  }

  private int do_close(long /* int */ browser) {
    if (!ChromiumLib.cefswt_is_same(Chromium.this.browser, browser)) {
      debugPrint("DoClose popup:" + Chromium.this.browser + ":" + browser);
      return 0;
    }
    Display display = Display.getDefault();
    if (/* !disposing && */ !isDisposed() && closeWindowListeners != null) {
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

  static void on_after_created(long /* int */ self, long /* int */ browser) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("on_after_created: " + id);
    if (browser != 0) {
      browsers.incrementAndGet();
    }

    safeGeInstance(id).on_after_created(browser);
  }

  private void on_after_created(long /* int */ browser) {
    if (isDisposed() || visibilityWindowListeners == null) {
      return;
    }
    debugPrint("on_after_created: " + browser);
    if (browser != 0) {
      Chromium.this.browser = browser;
      if (this.isPopup == null) {
        final org.eclipse.swt.graphics.Point size = getChromiumSize();
        ChromiumLib.cefswt_resized(browser, size.x, size.y);
      }
      if (this.isPopup != null && this.url != null) {
        debugPrint("load url after created");
        doSetUrlPost(browser, url, postData, headers);
      } else if (!"about:blank".equals(this.url)) {
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
    if (isDisposed() || visibilityWindowListeners == null) {
      return;
    }
    org.eclipse.swt.browser.WindowEvent event = new org.eclipse.swt.browser.WindowEvent(chromium);
    event.display = chromium.getDisplay();
    event.widget = chromium;
    event.size = new Point(0, 0);
    event.location = new Point(0, 0);
    if (isPopup != null) {
      event.size = isPopup.size;
      event.location = isPopup.location;
      event.addressBar = isPopup.addressBar;
      event.menuBar = isPopup.menuBar;
      event.statusBar = isPopup.statusBar;
      event.toolBar = isPopup.toolBar;

      if (event.size != null && !event.size.equals(new Point(0, 0))) {
        Point size = event.size;
        chromium.getShell().setSize(chromium.getShell().computeSize(size.x, size.y));
      }

      //              chromium.getDisplay().asyncExec(() -> {
      for (VisibilityWindowListener listener : visibilityWindowListeners) {
        listener.show(event);
      }
      //              });
    }
    //        });
    try {
      // not sleeping here causes deadlock with multiple window.open
      Thread.sleep(LOOP);
    } catch (InterruptedException e) {
      // Undefined behavior
    }
  }

  static int on_before_popup(long /* int */ self, long /* int */ browser, long /* int */ frame,
      long /* int */ target_url, long /* int */ target_frame_name, int target_disposition,
      int user_gesture, long /* int */ popupFeaturesPtr, long /* int */ windowInfo,
      long /* int */ client, long /* int */ settings, int no_javascript_access) {
    loopDisable = true;
    pumpDisable = true;
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("on_before_popup: " + id);
    int ret = safeGeInstance(id).on_before_popup(browser, popupFeaturesPtr, windowInfo, client);
    loopDisable = false;
    return ret;
  }

  private int on_before_popup(long /* int */ browser, long /* int */ popupFeaturesPtr,
      long /* int */ windowInfo, long /* int */ client) {
    if (isDisposed()) {
      return 1;
    }
    if (openWindowListeners == null) {
      return 0;
    }

    WindowEvent event = new WindowEvent(chromium);

    cef_popup_features_t popupFeatures = new cef_popup_features_t();
    ChromiumLib.memmove(popupFeatures, popupFeaturesPtr, ChromiumLib.cef_popup_features_t_sizeof());

    try {
      // not sleeping here causes deadlock with multiple window.open
      Thread.sleep(LOOP);
    } catch (InterruptedException e) {
      // Undefined behavior
    }
    chromium.getDisplay().syncExec(() -> {
      debugPrint("on_before_popup syncExec" + browser);
      event.display = chromium.getDisplay();
      event.widget = chromium;
      event.required = false;
      event.addressBar = popupFeatures.toolBarVisible == 1;
      event.menuBar = popupFeatures.menuBarVisible == 1;
      event.statusBar = popupFeatures.statusBarVisible == 1;
      event.toolBar = popupFeatures.toolBarVisible == 1;
      int x = popupFeatures.xSet == 1 ? popupFeatures.x : 0;
      int y = popupFeatures.ySet == 1 ? popupFeatures.y : 0;
      event.location = popupFeatures.xSet == 1 || popupFeatures.ySet == 1 ? new Point(x, y) : null;
      int width = popupFeatures.widthSet == 1 ? popupFeatures.width : 0;
      int height = popupFeatures.heightSet == 1 ? popupFeatures.height : 0;
      event.size =
          popupFeatures.widthSet == 1 || popupFeatures.heightSet == 1 ? new Point(width, height)
              : null;

      for (OpenWindowListener listener : openWindowListeners) {
        listener.open(event);
      }

      if (event.browser != null) {
        if (((Chromium) event.browser.webBrowser).instance == 0) {
          ((Chromium) event.browser.webBrowser).createPopup(windowInfo, client, event);
        } else {
          event.required = true;
        }
      } else {
        createDefaultPopup(windowInfo, client, event);
      }
    });

    if (event.browser == null && event.required) {
      return 1;
    }
    if (event.browser != null && event.required) {
      return 1;
    }
    return 0;
  }

  private void waitForClose(Display display) {
    if (display == null || display.isDisposed()) {
      return;
    }
    display.asyncExec(() -> {
      if (browser != 0) {
        waitForClose(display);
      }
    });
  }

  private static void set_load_handler() {
    loadHandler = CefFactory.newLoadHandler();
    loadHandler.on_loading_state_change_cb = new Callback(Chromium.class, "on_loading_state_change",
        void.class, new Type[] { long.class, long.class, int.class, int.class, int.class });
    loadHandler.on_loading_state_change = checkGetAddress(loadHandler.on_loading_state_change_cb);
    clientHandler.get_load_handler_cb =
        new Callback(Chromium.class, "get_load_handler", long.class, new Type[] { long.class });
    clientHandler.get_load_handler = checkGetAddress(clientHandler.get_load_handler_cb);
    loadHandler.ptr = C.malloc(cef_load_handler_t.sizeof);
    ChromiumLib.memmove(loadHandler.ptr, loadHandler, cef_load_handler_t.sizeof);
  }

  static long /* int */ get_load_handler(long /* int */ client) {
    //debug("GetLoadHandler");
    if (loadHandler == null) {
      return 0;
    }
    return loadHandler.ptr;
  }

  static void on_loading_state_change(long /* int */ self_, long /* int */ browser, int isLoading,
      int canGoBack, int canGoForward) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("on_loading_state_change: " + id);
    safeGeInstance(id).on_loading_state_change(browser, isLoading, canGoBack, canGoForward);
  }

  private void on_loading_state_change(long /* int */ browser, int isLoading, int canGoBack,
      int canGoForward) {
    Chromium.this.canGoBack = canGoBack == 1;
    Chromium.this.canGoForward = canGoForward == 1;
    if (isDisposed() || progressListeners == null) {
      return;
    }
    if (isLoading == 0) {
      for (BrowserFunction function : functions.values()) {
        if (function.index != 0) {
          if (!ChromiumLib.cefswt_function(browser, function.name, function.index)) {
            throw new SWTException("Cannot create BrowserFunction");
          }
        }
      }
    }
    updateText();
    if (isPopup != null) {
      textReady.thenRun(() -> enableProgress.complete(true));
    } else if (!enableProgress.isDone() && isLoading == 0) {
      textReady.thenRun(() -> {
        enableProgress.complete(true);
      });
      return;
    } else if (!enableProgress.isDone()) {
      return;
    }
    ProgressEvent event = new ProgressEvent(chromium);
    event.display = chromium.getDisplay();
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
  }

  private static void set_display_handler() {
    displayHandler = CefFactory.newDisplayHandler();
    displayHandler.on_title_change_cb = new Callback(Chromium.class, "on_title_change", void.class,
        new Type[] { long.class, long.class, long.class });
    displayHandler.on_title_change = checkGetAddress(displayHandler.on_title_change_cb);
    displayHandler.on_address_change_cb = new Callback(Chromium.class, "on_address_change",
        void.class, new Type[] { long.class, long.class, long.class, long.class });
    displayHandler.on_address_change = checkGetAddress(displayHandler.on_address_change_cb);
    displayHandler.on_status_message_cb = new Callback(Chromium.class, "on_status_message",
        void.class, new Type[] { long.class, long.class, long.class });
    displayHandler.on_status_message = checkGetAddress(displayHandler.on_status_message_cb);

    clientHandler.get_display_handler_cb =
        new Callback(Chromium.class, "get_display_handler", long.class, new Type[] { long.class });
    clientHandler.get_display_handler = checkGetAddress(clientHandler.get_display_handler_cb);
    displayHandler.ptr = C.malloc(cef_display_handler_t.sizeof);
    ChromiumLib.memmove(displayHandler.ptr, displayHandler, cef_display_handler_t.sizeof);
  }

  static long /* int */ get_display_handler(long /* int */ client) {
    //debug("GetDisplayHandler");
    if (displayHandler == null) {
      return 0;
    }
    return displayHandler.ptr;
  }

  static void on_title_change(long /* int */ self, long /* int */ browser, long /* int */ title) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("on_title_change: " + id);
    safeGeInstance(id).on_title_change(browser, title);
  }

  private void on_title_change(long /* int */ browser, long /* int */ title) {
    if (isDisposed() || titleListeners == null) {
      return;
    }
    String full_str = ChromiumLib.cefswt_cefstring_to_java(title);
    String str = getPlainUrl(full_str);
    TitleEvent event = new TitleEvent(chromium);
    event.display = chromium.getDisplay();
    event.widget = chromium;
    event.title = str;
    for (TitleListener listener : titleListeners) {
      listener.changed(event);
    }
  }

  static void on_address_change(long /* int */ self, long /* int */ browser, long /* int */ frame,
      long /* int */ url) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("on_address_change: " + id);
    safeGeInstance(id).on_address_change(browser, frame, url);
  }

  private void on_address_change(long /* int */ browser, long /* int */ frame, long /* int */ url) {
    //    debugPrint("on_address_change");
    if (isDisposed() || locationListeners == null) {
      return;
    }
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

  static void on_status_message(long /* int */ self, long /* int */ browser,
      long /* int */ status) {
    int id = ChromiumLib.cefswt_get_id(browser);
    //      debug("on_status_message: " + id);
    safeGeInstance(id).on_status_message(browser, status);
  }

  private void on_status_message(long /* int */ browser, long /* int */ status) {
    if (isDisposed() || statusTextListeners == null) {
      return;
    }
    String str = (status == 0) ? "" : ChromiumLib.cefswt_cefstring_to_java(status);
    StatusTextEvent event = new StatusTextEvent(chromium);
    event.display = chromium.getDisplay();
    event.widget = chromium;
    event.text = str;
    for (StatusTextListener listener : statusTextListeners) {
      listener.changed(event);
    }
  }

  private static void set_request_handler() {
    requestHandler = CefFactory.newRequestHandler();
    requestHandler.on_before_browse_cb = new Callback(Chromium.class, "on_before_browse", int.class,
        new Type[] { long.class, long.class, long.class, long.class, int.class, int.class });
    requestHandler.on_before_browse = checkGetAddress(requestHandler.on_before_browse_cb);
    requestHandler.get_auth_credentials_cb = new Callback(Chromium.class, "get_auth_credentials",
        int.class, new Type[] { long.class, long.class, long.class, int.class, long.class,
            int.class, long.class, long.class, long.class });
    requestHandler.get_auth_credentials = checkGetAddress(requestHandler.get_auth_credentials_cb);
    requestHandler.can_get_cookies_cb = new Callback(Chromium.class, "can_get_cookies", int.class,
        new Type[] { long.class, long.class, long.class, long.class });
    requestHandler.can_get_cookies = checkGetAddress(requestHandler.can_get_cookies_cb);
    requestHandler.can_set_cookie_cb = new Callback(Chromium.class, "can_set_cookie", int.class,
        new Type[] { long.class, long.class, long.class, long.class, long.class });
    requestHandler.can_set_cookie = checkGetAddress(requestHandler.can_set_cookie_cb);

    clientHandler.get_request_handler_cb =
        new Callback(Chromium.class, "get_request_handler", long.class, new Type[] { long.class });
    clientHandler.get_request_handler = checkGetAddress(clientHandler.get_request_handler_cb);
    requestHandler.ptr = C.malloc(cef_request_handler_t.sizeof);
    ChromiumLib.memmove(requestHandler.ptr, requestHandler, cef_request_handler_t.sizeof);
  }

  static long /* int */ get_request_handler(long /* int */ client) {
    //        debug("GetRequestHandler");
    if (requestHandler == null) {
      return 0;
    }
    return requestHandler.ptr;
  }

  static int on_before_browse(long /* int */ self, long /* int */ browser, long /* int */ frame,
      long /* int */ request, int user_gesture, int is_redirect) {
    int id = ChromiumLib.cefswt_get_id(browser);
    //      debug("on_before_browse: " + id);
    return safeGeInstance(id).on_before_browse(browser, frame, request);
  }

  private int on_before_browse(long /* int */ browser2, long /* int */ frame,
      long /* int */ request) {
    if (isDisposed() || locationListeners == null) {
      return 0;
    }
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
        debugPrint("canceled nav, dependats:" + enableProgress.getNumberOfDependents());
        enableProgress = new CompletableFuture<>();
      }
      return event.doit ? 0 : 1;
    }
    return 0;
  }

  static int get_auth_credentials(long /* int */ self, long /* int */ browser, long /* int */ frame,
      int isProxy, long /* int */ host, int port, long /* int */ realm, long /* int */ scheme,
      long /* int */ callback) {
    int id = ChromiumLib.cefswt_get_id(browser);
    //      debug("on_before_browse: " + id);
    return safeGeInstance(id).get_auth_credentials(browser, frame, host, port, realm, callback);
  }

  private int get_auth_credentials(long /* int */ browser2, long /* int */ frame,
      long /* int */ host, int port, long /* int */ realm, long /* int */ callback) {
    if (isDisposed()) {
      return 0;
    }

    AuthenticationEvent event = new AuthenticationEvent(chromium);
    event.display = chromium.getDisplay();
    event.widget = chromium;
    event.doit = true;
    String protocol = "http";
    try {
      URL u = new URL(this.url);
      protocol = u.getProtocol();
    } catch (MalformedURLException e) {
      // Undefined behavior
    }
    String hostStr = host != 0 ? ChromiumLib.cefswt_cefstring_to_java(host) : "";
    String realmStr = realm != 0 ? ChromiumLib.cefswt_cefstring_to_java(realm) : null;
    event.location = protocol + "://" + hostStr;
    debugPrint("get_auth_credentials:" + event.location);
    chromium.getDisplay().syncExec(() -> {
      for (AuthenticationListener listener : authenticationListeners) {
        listener.authenticate(event);
      }
      if (event.doit == true && event.user == null && event.password == null) {
        new AuthDialog(chromium.getShell()).open(event, realmStr);
      }
    });
    ChromiumLib.cefswt_auth_callback(callback, event.user, event.password, event.doit ? 1 : 0);
    return event.doit ? 1 : 0;
  }

  static int can_get_cookies(long /* int */ self, long /* int */ browser, long /* int */ frame,
      long /* int */ request) {
    return 1;
  }

  static int can_set_cookie(long /* int */ self, long /* int */ browser, long /* int */ frame,
      long /* int */ request, long /* int */ cookie) {
    return 1;
  }

  class AuthDialog extends Dialog {

    public AuthDialog(Shell parent) {
      super(parent);
    }

    public void open(AuthenticationEvent authEvent, String realm) {
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
        if (!display.readAndDispatch()) {
          display.sleep();
        }
      }
    }
  }

  private static void set_jsdialog_handler() {
    if (!"gtk".equals(SWT.getPlatform())) {
      return;
    }
    jsDialogHandler = CefFactory.newJsDialogHandler();
    jsDialogHandler.on_jsdialog_cb =
        new Callback(Chromium.class, "on_jsdialog", int.class, new Type[] { long.class, long.class,
            long.class, int.class, long.class, long.class, long.class, int.class });
    jsDialogHandler.on_jsdialog = checkGetAddress(jsDialogHandler.on_jsdialog_cb);

    clientHandler.get_jsdialog_handler_cb =
        new Callback(Chromium.class, "get_jsdialog_handler", long.class, new Type[] { long.class });
    clientHandler.get_jsdialog_handler = checkGetAddress(clientHandler.get_jsdialog_handler_cb);
    jsDialogHandler.ptr = C.malloc(cef_jsdialog_handler_t.sizeof);
    ChromiumLib.memmove(jsDialogHandler.ptr, jsDialogHandler, cef_jsdialog_handler_t.sizeof);
  }

  static long /* int */ get_jsdialog_handler(long /* int */ client) {
    //        debug("GetJSDialogHandler");
    if (jsDialogHandler == null) {
      return 0;
    }
    return jsDialogHandler.ptr;
  }

  static int on_jsdialog(long /* int */ self_, long /* int */ browser, long /* int */ origin_url,
      int dialog_type, long /* int */ message_text, long /* int */ default_prompt_text,
      long /* int */ callback, int suppress_message) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("on_jsdialog: " + id);
    return safeGeInstance(id).on_jsdialog(browser, origin_url, dialog_type, message_text,
        default_prompt_text, callback);
  }

  private int on_jsdialog(long /* int */ browser, long /* int */ origin_url, int dialog_type,
      long /* int */ message_text, long /* int */ default_prompt_text, long /* int */ callback) {
    if (isDisposed()) {
      return 0;
    }

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
      default:
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
    ChromiumLib.cefswt_dialog_close(callback, open == SWT.OK || open == SWT.YES ? 1 : 0,
        default_prompt_text);
    chromium.getShell().forceActive();
    return 1;
  }

  private static void set_context_menu_handler() {
    contextMenuHandler = CefFactory.newContextMenuHandler();
    contextMenuHandler.run_context_menu_cb =
        new Callback(Chromium.class, "run_context_menu", int.class,
            new Type[] { long.class, long.class, long.class, long.class, long.class, long.class });
    contextMenuHandler.run_context_menu = checkGetAddress(contextMenuHandler.run_context_menu_cb);

    clientHandler.get_context_menu_handler_cb = new Callback(Chromium.class,
        "get_context_menu_handler", long.class, new Type[] { long.class });
    clientHandler.get_context_menu_handler =
        checkGetAddress(clientHandler.get_context_menu_handler_cb);
    contextMenuHandler.ptr = C.malloc(cef_context_menu_handler_t.sizeof);
    ChromiumLib.memmove(contextMenuHandler.ptr, contextMenuHandler,
        cef_context_menu_handler_t.sizeof);
  }

  static long /* int */ get_context_menu_handler(long /* int */ client) {
    //        debug("GetContextMenuHandler");
    if (contextMenuHandler == null) {
      return 0;
    }
    return contextMenuHandler.ptr;
  }

  static int run_context_menu(long /* int */ self, long /* int */ browser, long /* int */ frame,
      long /* int */ params, long /* int */ model, long /* int */ callback) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("run_context_menu: " + id);
    return safeGeInstance(id).run_context_menu(browser, callback);
  }

  private int run_context_menu(long /* int */ browser2, long /* int */ callback) {
    if (chromium.getMenu() != null) {
      chromium.getMenu().setVisible(true);
      ChromiumLib.cefswt_context_menu_cancel(callback);
      return 1;
    }
    return 0;
  }

  private void updateText() {
    if (browser != 0 && !isDisposed() && disposing == 0) {
      debugPrint("update text");
      if (textVisitor != null) {
        textVisitor.refs++;
      } else {
        set_text_visitor();
      }
      textReady = new CompletableFuture<String>();
      ChromiumLib.cefswt_get_text(browser, textVisitor.ptr);
    }
  }

  private void set_text_visitor() {
    textVisitor = CefFactory.newStringVisitor();
    textVisitor.visit_cb =
        new Callback(this, "textVisitor_visit", void.class, new Type[] { long.class, long.class });
    textVisitor.visit = checkGetAddress(textVisitor.visit_cb);
    textVisitor.ptr = C.malloc(cef_string_visitor_t.sizeof);
    textVisitor.refs = 1;
    ChromiumLib.memmove(textVisitor.ptr, textVisitor, cef_string_visitor_t.sizeof);
  }

  void textVisitor_visit(long /* int */ self, long /* int */ cefString) {
    //    debugPrint("text visited");

    if (--textVisitor.refs == 0) {
      freeTextVisitor();
    }

    String newtext = cefString != 0 ? ChromiumLib.cefswt_cefstring_to_java(cefString) : null;
    if (newtext != null) {
      text = newtext;
      debugPrint("text visited completed");
      textReady.complete(text);
    } else {
      debugPrint("text visited null");
    }
  }

  private void freeTextVisitor() {
    debugPrint("free text visitor");
    disposeCallback(textVisitor.visit_cb);
    freeDelayed(textVisitor.ptr);
    textVisitor = null;
  }

  private static void set_keyboard_handler() {
    keyboardHandler = CefFactory.newKeyboardHandler();
    keyboardHandler.on_key_event_cb = new Callback(Chromium.class, "on_key_event", int.class,
        new Type[] { long.class, long.class, long.class, long.class });
    keyboardHandler.on_key_event = checkGetAddress(keyboardHandler.on_key_event_cb);

    clientHandler.get_keyboard_handler_cb =
        new Callback(Chromium.class, "get_keyboard_handler", long.class, new Type[] { long.class });
    clientHandler.get_keyboard_handler = checkGetAddress(clientHandler.get_keyboard_handler_cb);
    keyboardHandler.ptr = C.malloc(cef_keyboard_handler_t.sizeof);
    ChromiumLib.memmove(keyboardHandler.ptr, keyboardHandler, cef_keyboard_handler_t.sizeof);
  }

  static long /* int */ get_keyboard_handler(long /* int */ client) {
    //      debug("GetKeyboardHandler");
    if (keyboardHandler == null) {
      return 0;
    }
    return keyboardHandler.ptr;
  }

  static int on_key_event(long /* int */ keyboardHandler, long /* int */ browser,
      long /* int */ event, long /* int */ os_event) {
    int id = ChromiumLib.cefswt_get_id(browser);
    //      debug("on_key_event: " + id);
    return safeGeInstance(id).on_key_event(browser, event, os_event);
  }

  private enum cef_event_flags {
    NONE(0), CAPS_LOCK_ON(1), SHIFT_DOWN(2, SWT.SHIFT), CONTROL_DOWN(4, SWT.CONTROL),
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
      } else if (event.type == SWT.KeyUp && event.keyCode == swtKey && swtKey != SWT.NONE) {
        return true;
      }
      return false;
    }

    boolean isSet(int modifiers) {
      return (modifiers & cefFlag) != 0;
    }
  }

  private int on_key_event(long /* int */ browser, long /* int */ event, long /* int */ os_event) {
    cef_key_event_t e = new cef_key_event_t();
    ChromiumLib.memmove(e, event, ChromiumLib.cef_key_event_t_sizeof());

    Event firedEvent = new Event();
    if (e.type == 0 || e.type == 1) {
      firedEvent.type = SWT.KeyDown;
    } else if (e.type == 2) {
      firedEvent.type = SWT.KeyUp;
    } else {
      return 0;
    }

    int translateKey = e.windows_key_code == 91 || e.windows_key_code == 93 ? SWT.COMMAND
        : translateKey(e.windows_key_code);
    firedEvent.keyCode = translateKey != 0 ? translateKey : e.windows_key_code /* + 32 */;
    firedEvent.widget = chromium;
    firedEvent.character = e.character;
    firedEvent.display = chromium.getDisplay();
    int stateMask = 0;
    if (cef_event_flags.CAPS_LOCK_ON.shouldSetState(e.modifiers, firedEvent)) {
      stateMask += SWT.CAPS_LOCK;
    }
    if (cef_event_flags.SHIFT_DOWN.shouldSetState(e.modifiers, firedEvent)) {
      stateMask += SWT.SHIFT;
    }
    if (cef_event_flags.CONTROL_DOWN.shouldSetState(e.modifiers, firedEvent)) {
      stateMask += SWT.CTRL;
    }
    if (cef_event_flags.ALT_DOWN.shouldSetState(e.modifiers, firedEvent)) {
      stateMask += SWT.ALT;
    }
    if (cef_event_flags.COMMAND_DOWN.shouldSetState(e.modifiers, firedEvent)) {
      stateMask += SWT.COMMAND;
    }
    if (cef_event_flags.NUM_LOCK_ON.shouldSetState(e.modifiers, firedEvent)) {
      stateMask += SWT.NUM_LOCK;
    }
    if (cef_event_flags.IS_KEY_PAD.shouldSetState(e.modifiers, firedEvent)) {
      stateMask += SWT.KEYPAD;
    }
    if (cef_event_flags.IS_LEFT.shouldSetState(e.modifiers, firedEvent)) {
      stateMask += SWT.LEFT;
    }
    if (cef_event_flags.IS_RIGHT.shouldSetState(e.modifiers, firedEvent)) {
      stateMask += SWT.RIGHT;
    }
    firedEvent.stateMask = stateMask;

    //        System.out.println("e.windows_key_code: " + e.windows_key_code + " translated: "+ 
    //            translateKey(e.windows_key_code));
    //        System.out.println("e.native_key_code: " + e.native_key_code + " translated: "+ 
    //            translateKey(e.native_key_code));
    //        System.out.println("e.character: " + e.character + " e.unmodified_character: " + 
    //            e.unmodified_character);

    sendKeyEvent(firedEvent);

    if (firedEvent.doit && "cocoa".equals(SWT.getPlatform())) {
      int nativeHandleKey = ChromiumLib.cefswt_handlekey(browser, event);
      //        System.out.println("NativeHandleKey: "+nativeHandleKey);
      return nativeHandleKey;
    }
    return 0;
  }

  private static void set_focus_handler() {
    focusHandler = CefFactory.newFocusHandler();
    focusHandler.on_got_focus_cb = new Callback(Chromium.class, "on_got_focus", void.class,
        new Type[] { long.class, long.class });
    focusHandler.on_got_focus = checkGetAddress(focusHandler.on_got_focus_cb);
    focusHandler.on_set_focus_cb = new Callback(Chromium.class, "on_set_focus", int.class,
        new Type[] { long.class, long.class, int.class });
    focusHandler.on_set_focus = checkGetAddress(focusHandler.on_set_focus_cb);
    focusHandler.on_take_focus_cb = new Callback(Chromium.class, "on_take_focus", void.class,
        new Type[] { long.class, long.class, int.class });
    focusHandler.on_take_focus = checkGetAddress(focusHandler.on_take_focus_cb);

    clientHandler.get_focus_handler_cb =
        new Callback(Chromium.class, "get_focus_handler", long.class, new Type[] { long.class });
    clientHandler.get_focus_handler = checkGetAddress(clientHandler.get_focus_handler_cb);
    focusHandler.ptr = C.malloc(cef_focus_handler_t.sizeof);
    ChromiumLib.memmove(focusHandler.ptr, focusHandler, cef_focus_handler_t.sizeof);
  }

  static long /* int */ get_focus_handler(long /* int */ client) {
    //      debug("GetFocusHandler");
    if (focusHandler == null) {
      return 0;
    }
    return focusHandler.ptr;
  }

  static void on_got_focus(long /* int */ focusHandler, long /* int */ browser) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("on_got_focus: " + id);
    safeGeInstance(id).on_got_focus(browser);
  }

  private void on_got_focus(long /* int */ browser2) {
    if (!isDisposed()) {
      hasFocus = true;
      if (!isDisposed() && chromium.getDisplay().getFocusControl() != null) {
        chromium.setFocus();
      }
      browserFocus(true);
    }
  }

  static int on_set_focus(long /* int */ focusHandler, long /* int */ browser, int focusSource) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("on_set_focus: " + id);
    return safeGeInstance(id).on_set_focus(browser);
  }

  private int on_set_focus(long /* int */ browser) {
    if (ignoreFirstFocus) {
      ignoreFirstFocus = false;
      return 1;
    }
    return 0;
  }

  static void on_take_focus(long /* int */ focusHandler, long /* int */ browser, int next) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("on_take_focus: " + id);
    safeGeInstance(id).on_take_focus(browser, next);
  }

  private void on_take_focus(long /* int */ browser, int next) {
    hasFocus = false;
    Control[] tabOrder = chromium.getParent().getTabList();
    if (tabOrder.length == 0) {
      tabOrder = chromium.getParent().getChildren();
    }
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
          //                  debug("WORK CLOCK");
          safe_loop_work("timer");
          display.timerExec(LOOP * 2, loopWork);
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

  static int on_process_message_received(long /* int */ client, long /* int */ browser, int source,
      long /* int */ processMessage) {
    int id = ChromiumLib.cefswt_get_id(browser);
    debug("on_process_message_received: " + id);
    return safeGeInstance(id).on_process_message_received(browser, source, processMessage);
  }

  private int on_process_message_received(long /* int */ browser, int source,
      long /* int */ processMessage) {
    if (source != CefFactory.PID_RENDERER || !jsEnabled || disposing == 1 || isDisposed()) {
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
          chromium.getDisplay().readAndDispatch();
        } else {
          String value = ChromiumLib.cefswt_cstring_to_java(valuePtr);
          args[arg] = mapType(type, value);
        }
      };
      Callback callback_cb = new Callback(callback, "invoke", void.class,
          new Type[] { int.class, int.class, long.class });
      ChromiumLib.cefswt_function_arg(processMessage, i, checkGetAddress(callback_cb));
      disposeCallback(callback_cb);
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
    return new Object[] { returnType, returnStr };
  }

  protected void browserFocus(boolean set) {
    //debugPrint("cef focus: " + set);
    if (!isDisposed() && browser != 0) {
      long /* int */ parent =
          (Display.getDefault().getActiveShell() == null) ? 0 : getHandle(chromium.getParent());
      if (chromium.getDisplay().getActiveShell() != chromium.getShell()) {
        //              System.err.println("Ignore do_message_loop_work due inactive shell");
        return;
      }
      ChromiumLib.cefswt_set_focus(browser, set, parent);
    }
  }

  @Override
  public boolean close() {
    return super.close();
  }

  public void dispose() {
    if (disposing == 1 || isDisposed()) {
      return;
    }
    final boolean callClose = disposing != 2;
    disposing = 1;
    disposingAny++;
    if (focusListener != null) {
      chromium.removeFocusListener(focusListener);
    }
    focusListener = null;
    if (traverseListener != null) {
      chromium.removeListener(SWT.Traverse, traverseListener);
      chromium.removeListener(SWT.KeyDown, traverseListener);
      traverseListener = null;
    }
    if (browser != 0 && callClose) {
      //          browsers.decrementAndGet();
      debugPrint("call close_browser");
      ChromiumLib.cefswt_close_browser(browser);
    }
  }

  /**
   * Re-initializing CEF3 is not supported due to the use of globals. This must be
   * called on app exit.
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

  private static void internalShutdown() {
    if (lib == null || app == null) {
      return;
    }
    if (browsers.get() == 0) {
      debug("shutting down CEF on exit from thread " + Thread.currentThread().getName());
      freeAll(null);
      ChromiumLib.cefswt_shutdown();

      if (cookieVisitor != null) {
        disposeCallback(cookieVisitor.visit_cb);
        C.free(cookieVisitor.ptr);
        cookieVisitor = null;
      }

      disposeCallback(app.get_browser_process_handler_cb);
      C.free(app.ptr);
      app = null;

      disposeCallback(browserProcessHandler.on_schedule_message_pump_work_cb);
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
        throw new SWTException(SWT.ERROR_FAILED_LOAD_LIBRARY,
            "Chromium Browser is no longer supported in GTK2. ");
      }
    }

    String subDir = "chromium-" + CEFVERSION;
    File cefrustlib = null;
    try {
      String mapLibraryName = System.mapLibraryName(SHARED_LIB_V);
      String mapJniName = JNI_LIB_V;
      Enumeration<URL> fragments =
          Library.class.getClassLoader().getResources(subDir + "/chromium.properties");
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
              if (!mapLibraryName.equals(fileName) && !fileName.contains(mapJniName)) {
                ResourceExpander.findResource(path.getParent().toString(), fileName, false);
              }
            }
          }
        }
      }

      cefrustlib = ResourceExpander.findResource(subDir, mapLibraryName, false);
      final File jnilib = ResourceExpander.findResource(subDir, mapJniName, true);

      cefrustPath = cefrustlib.getParentFile().getCanonicalPath();

      CefFactory.create(cefrustPath);
      System.load(cefrustlib.toString());
      System.load(jnilib.toString());
      //          Library.loadLibrary(cefrustlib.toString(), false);
      //          Library.loadLibrary(jnilib.toString(), false);

      setupCookies();

      return new Object();
    } catch (UnsatisfiedLinkError e) {
      String cefLib = System.mapLibraryName("cef");
      if ("cocoa".equals(platform)) {
        cefLib = "Chromium Embedded Framework.framework";
      } else if ("win32".equals(platform)) {
        cefLib = "libcef.dll";
      }
      if (cefrustlib != null && !new File(cefrustlib.getParentFile(), cefLib).exists()) {
        SWTException swtError = new SWTException(SWT.ERROR_FAILED_LOAD_LIBRARY,
            "Missing CEF binaries for Chromium Browser. " + "Extract CEF binaries to "
                + cefrustPath);
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
        //                debug("CookieSet " + WebBrowser.CookieUrl + " " + cookie.getName() + 
        //                    " " + cookie.getValue() + " " + cookie.getDomain());
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
    cookieVisitor = CefFactory.newCookieVisitor();
    cookieVisitor.visit_cb = new Callback(Chromium.class, "cookieVisitor_visit", int.class,
        new Type[] { long.class, long.class, int.class, int.class, int.class });
    cookieVisitor.visit = checkGetAddress(cookieVisitor.visit_cb);

    cookieVisitor.ptr = C.malloc(cef_cookie_visitor_t.sizeof);
    ChromiumLib.memmove(cookieVisitor.ptr, cookieVisitor, cef_cookie_visitor_t.sizeof);
  }

  static int cookieVisitor_visit(long /* int */ self, long /* int */ cefcookie, int count,
      int total, int delete) {
    String name = ChromiumLib.cefswt_cookie_to_java(cefcookie);
    debug("Visitor " + count + "/" + total + ": " + name + ":" + Thread.currentThread());
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
      if (!enabled) {
        return;
      }
      enabled = false;
      //debugPrint("focusLost");
      browserFocus(false);
      // System.out.println(Display.getDefault().getFocusControl());
      enabled = true;
    }

    @Override
    public void focusGained(FocusEvent e) {
      if (!enabled) {
        return;
      }
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
        //debugPrint("eval retured: " +type + ":"+valuePtr);
        if (!(loopDisable
            && ("cocoa".equals(SWT.getPlatform()) || "gtk".equals(SWT.getPlatform())))) {
          chromium.getDisplay().readAndDispatch();
        }
        if (!loopDisable) {
          //             lib.cefswt_do_message_loop_work();
        }
      } else {
        String value = ChromiumLib.cefswt_cstring_to_java(valuePtr);
        debugPrint("eval returned: " + type + ":" + value);
        ret[0] = mapType(type, value);
      }
    };
    final Callback callback_cb = new Callback(callback, "invoke", void.class,
        new Type[] { int.class, int.class, long.class });

    StringBuilder buffer = new StringBuilder("(function() {");
    buffer.append("\n");
    buffer.append(script);
    buffer.append("\n})()");

    checkBrowser();
    boolean returnSt =
        ChromiumLib.cefswt_eval(browser, buffer.toString(), EVAL++, checkGetAddress(callback_cb));
    disposeCallback(callback_cb);
    if (!returnSt) {
      throw new SWTException("Script that was evaluated failed");
    }
    return ret[0];
  }

  private Object mapType(int type, String value) throws SWTException {
    if (type == ReturnType.Error.intValue()) {
      if ((SWT.ERROR_INVALID_RETURN_VALUE + "").equals(value)) {
        throw new SWTException(SWT.ERROR_INVALID_RETURN_VALUE);
      }
      throw new SWTException(SWT.ERROR_FAILED_EVALUATE, value);
    } else if (type == ReturnType.Null.intValue()) {
      return null;
    } else if (type == ReturnType.Bool.intValue()) {
      return "1".equals(value) ? Boolean.TRUE : Boolean.FALSE;
    } else if (type == ReturnType.Double.intValue()) {
      return Double.parseDouble(value);
    } else if (type == ReturnType.Array.intValue()) {
      String value_unquoted = value.substring(1, value.length() - 1);
      String[] elements = value_unquoted.split(";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
      Object[] array = new Object[elements.length];
      for (int i = 0; i < array.length; i++) {
        String elemUnquoted = elements[i].substring(1, elements[i].length() - 1);
        String[] parts = elemUnquoted.split(",(?=(?:[^']*'[^']*')*[^']*$)", 2);
        ReturnType elemType = CefFactory.ReturnType.from(parts[0]);
        Object elemValue = mapType(elemType.intValue(), parts[1]);
        array[i] = elemValue;
      }
      return array;
    } else {
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
    long /* int */ urlPtr = ChromiumLib.cefswt_get_url(browser);
    String cefurl = null;
    if (urlPtr != 0) {
      cefurl = ChromiumLib.cefswt_cstring_to_java(urlPtr);
    }
    //        debugPrint("getUrl1:" + cefurl);
    if (cefurl == null) {
      cefurl = getPlainUrl(this.url);
    }
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
      return url.substring(0, DATA_TEXT_URL.length() - 8);
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

  private static void doSetUrlPost(long /* int */ browser, String url, String postData,
      String[] headers) {
    byte[] bytes = (postData != null) ? postData.getBytes(Charset.forName("ASCII")) : null;
    int bytesLength = (postData != null) ? bytes.length : 0;
    int headersLength = (headers != null) ? headers.length : 0;
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

  private static Chromium safeGeInstance(int id) {
    Chromium c = instances.get(id);
    if (c == null) {
      throw new SWTError("Wrong chromium id " + id);
    }
    return c;
  }

  private static void freeDelayed(long /* int */ ptr) {
    Display.getDefault().asyncExec(() -> C.free(ptr));
  }

  //    static int cbs = 0;
  static long /* int */ checkGetAddress(Callback cb) {
    long /* int */ address = cb.getAddress();
    //      cbs++;
    if (address == 0) {
      throw new SWTError(SWT.ERROR_NO_HANDLES);
    }
    //      debug("CALLBACKS "+cbs);
    return address;
  }

  static void disposeCallback(Callback cb) {
    if (cb != null) {
      cb.dispose();
    }
    //      cbs--;
  }

  public static interface EvalReturned {
    void invoke(int loop, int type, long /* int */ value);
  }

}
