package com.equo.chromium.utils;

import java.util.Map;

public class EventAction implements Runnable {
	protected Map<String,Object> mapData;

	public EventAction setJsonData(Map<String,Object> mapData) {
		this.mapData = mapData;
		return this;
	}

	@Override
	public void run() {

	}

}
