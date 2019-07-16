package org.eclipse.swt.internal.chromium;

///
/// Popup window features.
///
public class cef_popup_features_t {
	public int x;
	public int xSet;
	public int y;
	public int ySet;
	public int width;
	public int widthSet;
	public int height;
	public int heightSet;
	public int menuBarVisible;
	public int statusBarVisible;
	public int toolBarVisible;
	public int locationBarVisible;
	public int scrollbarsVisible;
	public int resizable;
	public int fullscreen;
	public int dialog;

	public static final int sizeof = ChromiumLib.cef_popup_features_t_sizeof();
}