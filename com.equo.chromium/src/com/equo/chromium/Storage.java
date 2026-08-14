/****************************************************************************
**
** Copyright (C) 2026 Equo
**
** This file is part of Equo Chromium.
**
** Commercial License Usage
** Licensees holding valid commercial Equo licenses may use this file in
** accordance with the commercial license agreement provided with the
** Software or, alternatively, in accordance with the terms contained in
** a written agreement between you and Equo. For licensing terms
** and conditions see https://www.equo.dev/terms.
**
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3 as published by the Free Software
** Foundation. Please review the following
** information to ensure the GNU General Public License requirements will
** be met: https://www.gnu.org/licenses/gpl-3.0.html.
**
****************************************************************************/
package com.equo.chromium;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.equo.chromium.internal.IndependentBrowser;
import com.equo.chromium.utils.StorageType;

public class Storage {
	private IndependentBrowser browser;
	private final boolean isLocalStorage;

	public Storage(IndependentBrowser browser, StorageType st) {
		this.browser = browser;
		this.isLocalStorage = StorageType.LOCAL.equals(st);
	}

	private static String extractOrigin(String fullUrl) {
		URI uri = null;
		try {
			uri = new URI(fullUrl);
		} catch (URISyntaxException e) {
		}

		String scheme = uri.getScheme();
		String host = uri.getHost();
		int port = uri.getPort();

		if (port == -1) {
			return scheme + "://" + host;
		} else {
			return scheme + "://" + host + ":" + port;
		}
	}

	/**
	 * Get saved data from the Storage as a CompletableFuture<String>
	 * 
	 * @param key A string containing the name of the key you want to retrieve the
	 *            value of.
	 * @return A CompletableFuture<String> containing the value of the key. If the
	 *         key does not exist, null is returned.
	 */
	public CompletableFuture<String> getItem(String key) {
		List<Map.Entry<String, Object>> params = new ArrayList<>();
		params.add(new AbstractMap.SimpleEntry<>("storageId", getStorageId()));
		params.add(new AbstractMap.SimpleEntry<>("key", key));

		return browser.sendDevToolsMessage("DOMStorage.getDOMStorageItems", params, "entries").thenApply(response -> {
			if (response instanceof List) {
				@SuppressWarnings("unchecked")
				List<List<String>> entries = (List<List<String>>) response;
				for (List<String> entry : entries) {
					if (entry.get(0).equals(key)) {
						return entry.get(1);
					}
				}
			}
			return null;
		});
	}

	private Map<String, Object> getStorageId() {
		Map<String, Object> storageId = new HashMap<>();
		storageId.put("securityOrigin", extractOrigin(browser.getUrl()));

		storageId.put("isLocalStorage", isLocalStorage);
		return storageId;
	}

	/**
	 * Save data to the Storage
	 * 
	 * @param key   A string containing the name of the key you want to
	 *              create/update.
	 * @param value A string containing the value you want to give the key you are
	 *              creating/updating.
	 */
	public CompletableFuture<Void> setItem(String key, String value) {
		List<Map.Entry<String, Object>> params = new ArrayList<>();
		params.add(new AbstractMap.SimpleEntry<>("storageId", getStorageId()));
		params.add(new AbstractMap.SimpleEntry<>("value", value));
		params.add(new AbstractMap.SimpleEntry<>("key", key));

		return browser.sendDevToolsMessage("DOMStorage.setDOMStorageItem", params, "").thenApply(response -> null);
	}

	/**
	 * Remove saved data from the Storage
	 * 
	 * @param key A string containing the name of the key you want to remove.
	 */
	public void remove(String key) {
		List<Map.Entry<String, Object>> params = new ArrayList<>();
		params.add(new AbstractMap.SimpleEntry<>("storageId", getStorageId()));
		params.add(new AbstractMap.SimpleEntry<>("key", key));

		browser.sendDevToolsMessage("DOMStorage.removeDOMStorageItem", params, "");
	}

	/**
	 * Remove all saved data from the Storage
	 */
	public void clear() {
		List<Map.Entry<String, Object>> params = new ArrayList<>();
		params.add(new AbstractMap.SimpleEntry<>("storageId", getStorageId()));

		browser.sendDevToolsMessage("DOMStorage.clear", params, "");
	}
}