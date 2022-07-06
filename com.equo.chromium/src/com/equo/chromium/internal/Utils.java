package com.equo.chromium.internal;

public class Utils {
	private static String OS = System.getProperty("os.name").toLowerCase();

	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	public static boolean isMac() {
		return (OS.indexOf("mac") >= 0);
	}

	public static String getWindowing() {
		if (isWindows())
			return "win32";
		if (isMac())
			return "cocoa";
		return "gtk";
	}

	public static String getOS() {
		if (isWindows())
			return "win32";
		if (isMac())
			return "macosx";
		return "linux";
	}

}
