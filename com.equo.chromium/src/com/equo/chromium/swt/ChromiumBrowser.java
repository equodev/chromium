package com.equo.chromium.swt;

import java.util.List;

public interface ChromiumBrowser {

	/**
	 * @param search Pass null to clean search
	 */
	public void find(String search, boolean forward, boolean matchCase);
	
	/**
	 * Change the zoom level to the specified value. Specify 0.0 to reset the zoom level.
	 * @param zoomLevel
	 */
	public void zoom(double zoomLevel);
	
	public void addConsoleListener(ConsoleListener listener);
	
	public void removeConsoleListener(ConsoleListener listener);
	
	@FunctionalInterface
	public interface ConsoleListener {
		/**
		 * level: 2 (INFO), 3 (WARNING), 4 (ERROR).
		 * @return true to stop message from being output to console.
		 */
		boolean message(int level, String message, String source, int line);
	}

	List<Object> getErrors();

	/**
	 * Early load and init the chromium engine and libraries.
	 * Usually not required.
	 */
	public static void earlyInit() throws ClassNotFoundException {
		Class.forName("com.equo.chromium.swt.internal.Engine");
	}
}
