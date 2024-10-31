/****************************************************************************
**
** Copyright (C) 2024 Equo
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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import com.equo.chromium.internal.IndependentBrowser;
import com.equo.chromium.utils.StorageType;

public class Storage {
	private final String storage;
	private final String setStorageFunction;
	private final String getStorageFunction;
	private final String removeStorageFunction;
	private final String clearStorageFunction;
	private IndependentBrowser browser;

	public Storage(IndependentBrowser browser, StorageType st) {
		this.browser = browser;
		storage = "window." + st.name().toLowerCase() + "Storage";
		setStorageFunction = storage + ".setItem";
		getStorageFunction = storage + ".getItem";
		removeStorageFunction = storage + ".removeItem";
		clearStorageFunction = storage + ".clear";
	}

	private CompletableFuture<Object> sendDevToolsMessage(String script, String wanted) {
		List<Entry<String, Object>> params = new ArrayList<>();
		params.add(new SimpleEntry<>("expression", script));
		return browser.sendDevToolsMessage("Runtime.evaluate", params, wanted);
	}

	/**
	 * Save data to the Storage
	 * 
	 * @param key   A string containing the name of the key you want to
	 *              create/update.
	 * @param value A string containing the value you want to give the key you are
	 *              creating/updating.
	 */
	public void setItem(String key, String value) {
		String script = setStorageFunction + "('" + key + "','" + value + "')";
		sendDevToolsMessage(script, null);
	}

	/**
	 * Get saved data from the Storage as a CompletableFuture<String>
	 * 
	 * @param key A string containing the name of the key you want to retrieve the
	 *            value of.
	 * @return A CompletableFuture<String> containing the value of the key. If the
	 *         key does not exist, null is returned.
	 */
	@SuppressWarnings("unchecked")
	public CompletableFuture<String> getItem(String key) {
		String script = getStorageFunction + "('" + key + "')";
		return sendDevToolsMessage(script, "result").thenApply(json -> ((Map<String, String>) json).get("value"));
	}

	/**
	 * Remove saved data from the Storage
	 * 
	 * @param key A string containing the name of the key you want to remove.
	 */
	public void remove(String key) {
		String script = removeStorageFunction + "('" + key + "')";
		sendDevToolsMessage(script, null);
	}

	/**
	 * Remove all saved data from the Storage
	 */
	public void clear() {
		String script = clearStorageFunction + "()";
		sendDevToolsMessage(script, null);
	}
}
