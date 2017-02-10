package com.make.swtcef;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class Chromium extends Canvas {

    private static String OS = System.getProperty("os.name").toLowerCase();
    
    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    public static boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
    }

	public Chromium(Composite parent, int style) {
		super(parent, SWT.EMBEDDED);
		
		long hwnd = 0;
		if (isWindows()) {
			try {
				Field field = Control.class.getDeclaredField("handle");
				field.setAccessible(true);
				hwnd = (long) field.get(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (isMac()) {
			try {
				Field field = Control.class.getDeclaredField("view");
				Object nsview = field.get(this);
				
				Method windowsM = nsview.getClass().getMethod("window");
				Object win = windowsM.invoke(nsview);

				//long winNro = (long) win.getClass().getMethod("windowNumber").invoke(win);
				//hwnd = winNro;

				//long winRef = (long) win.getClass().getMethod("windowRef").invoke(win);
				//hwnd = winRef;

				Class<?> idClass = win.getClass().getSuperclass().getSuperclass().getSuperclass();
				System.out.println("IDCLASS: " + idClass.getName());
				Field idField = idClass.getField("id");
				
				long winId = idField.getLong(win);

				hwnd = idField.getLong(nsview);

				//setSharingType(winId, 2);

				//Object contentView = win.getClass().getMethod("contentView").invoke(win);
				//hwnd = idField.getLong(contentView);

				//System.out.println("sharingType:" + getSharingType(winId));

				//hwnd = winId;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				Field field = Composite.class.getDeclaredField("embeddedHandle");
				field.setAccessible(true);
				hwnd = (long) field.get(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
//		long hwnd = embeddedHandle;
		System.err.println("jhwnd: " + hwnd);
		ProcessBuilder b = new ProcessBuilder("/Users/guille/ws/cefrust/target/debug/cefrust.app/Contents/MacOS/cefrust", hwnd + "");
//		ProcessBuilder b = new ProcessBuilder("/home/guille/workspaces/rust/cefrust/target/debug/cefrust", hwnd + "");
//		ProcessBuilder b = new ProcessBuilder("F:\\rust\\cefrust\\target\\debug\\cefrust.exe", hwnd + "");
//		b.directory(new File("/home/guille/workspaces/rust/cefrust/target/debug/"));
//		b.environment().put("LD_LIBRARY_PATH", "/home/guille/workspaces/rust/cefrust/target/debug/");
		b.inheritIO();
		System.out.println(b.command());
		try {
			Process p = b.start();
//			if (p.waitFor() == 0) {
//				System.err.println("launched");
//			} else {
//				System.err.println("launched error: " + p.exitValue());
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
/*
	private static final long sel_setSharingType_ = org.eclipse.swt.internal.cocoa.OS.sel_registerName("setSharingType:");
	private static final long sel_sharingType = org.eclipse.swt.internal.cocoa.OS.sel_registerName("sharingType");

	public static void setSharingType(long id, int sharingType) {
		org.eclipse.swt.internal.cocoa.OS.objc_msgSend(id, sel_setSharingType_, sharingType);
	}

	public static long getSharingType(long id) {
		return org.eclipse.swt.internal.cocoa.OS.objc_msgSend(id, sel_sharingType);
	}
*/
}
