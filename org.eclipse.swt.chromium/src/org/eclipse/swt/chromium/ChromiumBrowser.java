package org.eclipse.swt.chromium;

public interface ChromiumBrowser {

	/**
	 * @param search Pass null to clean search
	 */
	public void find(String search, boolean forward, boolean matchCase);
	
}
