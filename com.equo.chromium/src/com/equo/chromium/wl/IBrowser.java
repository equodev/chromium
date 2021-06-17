package com.equo.chromium.wl;

import com.equo.chromium.swt.internal.Windowless;

public interface IBrowser {
	boolean setUrl(String url);

	Object evaluate(String script);
	
	boolean close();
	
	static IBrowser windowless(String url) {
		return new Windowless(url);
	}
}
