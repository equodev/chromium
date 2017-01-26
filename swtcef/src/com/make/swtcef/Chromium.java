package com.make.swtcef;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

import cef.capi.CEF;
import cef.capi.CEF.Client;
import cef.capi.CEF.FocusHandler;
import cef.capi.CEF.FocusSource;
import jnr.ffi.LibraryLoader;
import jnr.ffi.NativeType;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.TypeAlias;
import jnr.ffi.annotations.Direct;
import jnr.ffi.annotations.Out;
import jnr.ffi.annotations.Pinned;

public class Chromium extends Canvas {

	private static Lib lib;
	private static Pointer app;
	private static int browsers = 0;
	
	private long hwnd;
	private Pointer browser;
	private CEF.FocusHandler focusHandler;
	private CEF.Client clientHandler;
	private FocusListener focusListener;
	
	static {
		lib = loadLib();
	}

	public Chromium(Composite parent, int style) {
		super(parent, style);
//		Field field;
//		try {
//			field = Widget.class.getDeclaredField("handle");
//			Object handleObj = field.get(this);
//			if (handleObj instanceof Long)
//				hwnd = (Long) handleObj;
//			else
//				hwnd = (Integer) handleObj;
//		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
//			throw new RuntimeException(e);
//		}
		hwnd = handle;
		
		
//		Runtime runtime = jnr.ffi.Runtime.getRuntime(lib);
		synchronized (lib) {
			if (app == null) {
				app = lib.init();
			}
			browsers++;
		}
		
		System.out.println("HWND1: " + hwnd);
//		String url = "http://www.lanacion.com.ar";
		String url = "http://www.google.com";
//		String url = "http://www.keyboardtester.com/tester.html";
		
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				System.out.println("disposing chromium");
				dispose();
			}
		});
		focusListener = new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				removeFocusListener(focusListener);
				System.out.println("focusLost");
				browserFocus(false);
//				System.out.println(Display.getDefault().getFocusControl());
				addFocusListener(focusListener);
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				System.out.println("focusGained");
				browserFocus(true);
			}
		};
		addFocusListener(focusListener);
		
		clientHandler = new CEF.Client(CEF.RUNTIME);
		//initializeClientHandler(clientHandler);
		focusHandler = new CEF.FocusHandler(CEF.RUNTIME);
		focusHandler.setOnGotFocus((focusHandler, browser_1) -> {
			DEBUG_CALLBACK("CALLBACK OnGotFocus");
			if (!isFocusControl()) {
				removeFocusListener(focusListener);
				boolean r = forceFocus();
				browserFocus(true);
				addFocusListener(focusListener);
				System.out.println("Forcing focus to SWT canvas: " + r);
			}
		});
		focusHandler.setOnSetFocus((focusHandler, browser_1, focusSource_2) -> {
			DEBUG_CALLBACK("CALLBACK OnSetFocus " + focusSource_2);
			if (!isFocusControl()) {
				System.out.println("Disallowing focus to SWT canvas");
				//removeFocusListener(focusListener);
				//setFocus();
				//addFocusListener(focusListener);
				return 1;
			}
			System.out.println("Allowing focus to SWT canvas");
			return 0;
		});
		focusHandler.setOnTakeFocus((focusHandler, browser_1, next) -> {
			DEBUG_CALLBACK("CALLBACK OnTakeFocus " + next);
			Control[] tabOrder = getParent().getTabList();
			if (tabOrder.length == 0)
				tabOrder = getParent().getChildren();
			int indexOf = Arrays.asList(tabOrder).indexOf(Chromium.this);
			if (indexOf != -1) {
				int newIndex = (next == 1) ? indexOf + 1 : indexOf - 1;
				if (newIndex > 0 && newIndex < tabOrder.length && !tabOrder[newIndex].isDisposed()) {
					tabOrder[newIndex].setFocus();
					return;
				}
			}
			if (!getParent().isDisposed()) {
				getParent().setFocus();
			}
		});
		clientHandler.setGetFocusHandler(new CEF.Client.GetFocusHandler() {
			@Override
			public FocusHandler invoke(Pointer client) {
				DEBUG_CALLBACK("GetFocusHandler");
				return focusHandler;
			}
		});
		
		browser = lib.create_browser(hwnd, url, clientHandler);

		final Display display = parent.getDisplay();
//		lib.do_message_loop_work();
		if (browsers == 1) {
			doMessageLoop(display);
		}
		addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				if (!isDisposed() && browser != null) {
					lib.resized(browser, getSize().x, getSize().y);
				}
			}
			
			@Override
			public void controlMoved(ControlEvent e) {
			}
		});
	}

	public void setUrl(String url) {
		if (!isDisposed() && browser != null) {
			lib.load_url(browser, url);
		}
	}

	// single loop for all browsers
	private static void doMessageLoop(final Display display) {
		final int loop = 15;
		display.timerExec(loop, new Runnable() {
			public void run() {
				if (lib != null && browsers > 0) {
//					System.out.println("loop");
					lib.do_message_loop_work();
					display.timerExec(loop, this);
				}
		}});
	}
	
	private static jnr.ffi.Pointer DEBUG_CALLBACK(String log) {
		System.out.println("J:" + log);
		return null;
	}
	
	protected static void initializeClientHandler(Client client) {
		System.out.println("initialize_client_handler");
		// callbacks
		client.setGetContextMenuHandler((c) -> DEBUG_CALLBACK("get_context_menu_handler"));
		client.setGetDialogHandler((c) -> DEBUG_CALLBACK("get_dialog_handler"));
		client.setGetDisplayHandler((c) -> DEBUG_CALLBACK("get_display_handler"));
		client.setGetDownloadHandler((c) -> DEBUG_CALLBACK("get_download_handler"));
		client.setGetDragHandler((c) -> DEBUG_CALLBACK("get_drag_handler"));
		client.setGetFocusHandler((c) -> {DEBUG_CALLBACK("get_drag_handler"); return null;});
		client.setGetGeolocationHandler((c) -> DEBUG_CALLBACK("get_geolocation_handler"));
		client.setGetJsdialogHandler((c) -> DEBUG_CALLBACK("get_jsdialog_handler"));
		client.setGetKeyboardHandler((c) -> DEBUG_CALLBACK("get_keyboard_handler"));
		client.setGetLifeSpanHandler((c) -> DEBUG_CALLBACK("get_life_span_handler"));
		client.setGetLoadHandler((c) -> DEBUG_CALLBACK("get_load_handler"));
		client.setGetRenderHandler((c) -> DEBUG_CALLBACK("get_render_handler"));
		client.setGetRequestHandler((c) -> DEBUG_CALLBACK("get_request_handler"));
		client.setOnProcessMessageReceived((c, browser_1, processId_2, processMessage_3) -> {
			DEBUG_CALLBACK("on_process_message_received");
			return 0;
		});
		client.setGetFindHandler(c -> DEBUG_CALLBACK("setGetFindHandler"));
	}
	
//	@Override
//	public boolean forceFocus() {
//		System.out.println("focus");
//		boolean forceFocus = super.forceFocus();
//		if (forceFocus) {
//			browserFocus(true);
//		}
//		return forceFocus;
//	}
//	
//	@Override
//	public boolean setFocus() {
//		// TODO Auto-generated method stub
//		return super.setFocus();
//	}
	
	protected void browserFocus(boolean set) {
		System.out.println("cef focus: " + set);
		if (!isDisposed() && browser != null) {
			long parent = (Display.getDefault().getActiveShell() == null) ? 0 : getParent().handle;
			lib.set_focus(browser, set, parent);
		}
	}
	
	@Override
	public void dispose() {
		if (browser != null) {
			lib.try_close_browser(browser);
			browser = null;
			focusHandler = null;
			clientHandler = null;
			removeFocusListener(focusListener);
			focusListener = null;
			browsers--;
		}
		if (browsers == 0) {
			//System.out.println("shutting down CEF");
			// TODO delete app, free object on rust
			//lib.shutdown();
			app = null;
		}
		super.dispose();
	}
	
	private static Lib loadLib() {
		Lib libc = LibraryLoader.create(Lib.class).load("cefrustlib");
		
		java.lang.Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				if (lib != null) {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							System.out.println("shutting down CEF");
							// TODO delete app
							lib.shutdown();
							lib = null;
						};
					});
				}
			}
		}));
		return libc;
	}
	
	public static interface Lib {
		Pointer init();
		Pointer create_browser(long hwnd, String url, CEF.Client clientHandler);
		void do_message_loop_work();
		Pointer create_browser(long hwnd, String url);
		void load_url(Pointer browser, String url);
		void resized(Pointer browser, int width, int height);
		void set_focus(Pointer browser, boolean focus, long shell_hwnd);
		void try_close_browser(Pointer browser);
		void shutdown();
	}
}
