package org.eclipse.swt.internal.chromium.lib;

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
	public int scrollbarsVisible;

	public static final int sizeof = ChromiumLib.cef_popup_features_t_sizeof();
}