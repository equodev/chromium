package com.make.swtcef;

import java.lang.reflect.Field;

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

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

	private long hwnd;
	private Lib lib;
	private Lib.App app;

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
		
		System.out.println("HWND1: " + hwnd);
		lib = loadLib();
		
//		new Thread() {
//			public void run() {
		Runtime runtime = jnr.ffi.Runtime.getRuntime(lib);
//		System.err.println("SIZEOF: " + Struct.size(new Lib.App(runtime)));
				app = lib.init(hwnd);
				//System.err.println("app: " + app);
//				lib.do_message_loop_work();
//			};
//		}.start();
		final Display display = parent.getDisplay();
//		lib.do_message_loop_work();
		final int loop = 15;
		display.timerExec(loop, new Runnable() {
			public void run() {
				if (!isDisposed()) {
					//System.out.println("JAVA: do_message_loop_work");
					lib.do_message_loop_work();
					display.timerExec(loop, this);
				}
		}});
		addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
//							System.out.println("JAVA RESIZED");
				if (!isDisposed()) {
					lib.resized(app, getSize().x, getSize().y);
				}
			}
			
			@Override
			public void controlMoved(ControlEvent e) {
			}
		});
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				System.out.println("disposing");
				dispose();
			}
		});
	}
	
	@Override
	public void dispose() {
		lib.shutdown();
		super.dispose();
	}
	
	private Lib loadLib() {
		Lib libc = LibraryLoader.create(Lib.class).load("cefrustlib");
		return libc;
	}
	
	public static interface Lib {
		App init(long hwnd);
		void do_message_loop_work();
		void resized(App app, int width, int height);
		void shutdown();
		
		public static class App extends Struct {
			Unsigned64 canvas_hwnd = new Unsigned64();
			Padding cef_app = new Padding(getRuntime().findType(NativeType.VOID), 72);
			Padding browser = new Padding(getRuntime().findType(NativeType.VOID), 16);

			public App(Runtime runtime) {
				super(runtime);
			}
			
		}
	}
}
