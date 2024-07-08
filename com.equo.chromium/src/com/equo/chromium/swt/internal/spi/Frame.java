package com.equo.chromium.swt.internal.spi;

public class Frame {

	private String currentUrl;
	private String name;
	private boolean isMain;

	public Frame(String url, String name, boolean isMain) {
		this.currentUrl = url;
		this.name = name;
		this.isMain = isMain;
	}

	public String getCurrentUrl() {
		return currentUrl;
	}

	public String getName() {
		return name;
	}

	public boolean isMain() {
		return isMain;
	}
}
