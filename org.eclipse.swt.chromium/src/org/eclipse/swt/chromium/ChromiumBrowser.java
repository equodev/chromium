package org.eclipse.swt.chromium;

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
}
