package com.make.swtcef;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
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
			//long /*int*/ hwndChild = org.eclipse.swt.internal.win32.OS.GetWindow (handle, org.eclipse.swt.internal.win32.OS.GW_CHILD);
			hwnd = handle;
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
		ProcessBuilder b = new ProcessBuilder("/home/guille/workspaces/rust/cefrust/target/debug/cefrust", hwnd + "");
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

}
