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
package com.equo.chromium.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.cef.CefSettings.LogSeverity;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefStringVisitor;

import com.equo.chromium.ChromiumBrowser;
import com.equo.chromium.utils.EventAction;
import com.equo.chromium.utils.EventType;

public class Subscriber {
	private IndependentBrowser _browser;
	private Map<EventType, List<EventAction>> subscribeEvents = new HashMap<>();
	private Map<Long, ActionData> subscribeIndex = new HashMap<>();
	private long eventId = 0;
	private boolean firstLoading = true;
	private int _errorCode = 0;
	protected static EventAction eventActionOfAfterCreated = null;

	class ActionData {
		public EventType eventType;
		public Runnable action;

		public ActionData(EventType eventType, Runnable action) {
			this.eventType = eventType;
			this.action = action;
		}
	}

	public Subscriber(IndependentBrowser browser) {
		_browser = browser;
	}

	public void dispose() {
		_browser = null;
		unSubscribeAll();
	}

	public static void subscribeOnAfterCreated(EventAction eventAction) {
		eventActionOfAfterCreated = eventAction;
	}

	public static EventAction getEventActionOfAfterCreated() {
		return eventActionOfAfterCreated;
	}

	public synchronized long subscribe(EventType eventType, EventAction action) {
		_browser.isCreated().thenRun(() -> {
			subscribeEvents.computeIfAbsent(eventType, m -> new ArrayList<EventAction>()).add(action);
			subscribeIndex.put(eventId, new ActionData(eventType, action));
		});
		return eventId++;
	}

	public synchronized boolean unSubscribe(long idEvent) {
		_browser.isCreated().thenRun(() -> {
			ActionData actionData = subscribeIndex.get(idEvent);
			if (actionData != null) {
				subscribeEvents.get(actionData.eventType).remove(actionData.action);
				subscribeIndex.remove(idEvent);
			}
		});
		return false;
	}

	public synchronized void unSubscribeAll() {
		subscribeIndex.clear();
		subscribeEvents.clear();
	}

	protected void notifySubscribers(EventType eventType) {
		notifySubscribers(eventType, new HashMap<String, Object>());
	}

	protected synchronized void notifySubscribers(EventType eventType, Map<String, Object> mapData) {
		subscribeEvents.computeIfAbsent(eventType, m -> new ArrayList<EventAction>()).forEach(e -> {
			e.setJsonData(mapData);
			CompletableFuture.runAsync(e);
		});
	}

	public void onAfterCreatedNotify(CefBrowser browser) {
		if (eventActionOfAfterCreated != null && !_browser.isCreated().isDone()) {
			Map<String, Object> mapData = new HashMap<>();
			mapData.put("chromium_instance", (ChromiumBrowser)browser.getReference());
			eventActionOfAfterCreated.setJsonData(mapData);
			eventActionOfAfterCreated.run();
		}
		_browser.isCreated().complete(true);
		notifySubscribers(EventType.onAfterCreated);
	}

	public void onLoadEndNotify(CefFrame frame) {
		CefFrame parentFrame = frame.getParent();
		final boolean isMain = frame.isMain();
		final String parentId = parentFrame != null ? parentFrame.getIdentifier() : "0";
		frame.getSource(new CefStringVisitor() {
			@Override
			public void visit(String source) {
				String jsonSource = source.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n");
				Map<String, Object> mapData = new HashMap<>();
				mapData.put("isMain", isMain);
				mapData.put("html", jsonSource);
				mapData.put("name", frame.getName());
				mapData.put("id", frame.getIdentifier());
				mapData.put("parentId", parentId);
				notifySubscribers(EventType.onLoadEnd, mapData);
			}
		});
	}

	public void onLoadingStateChangeNotify(boolean isLoading, String url) {
		if (firstLoading || isLoading) {
			notifySubscribers(EventType.onLoadingStateChange);
			firstLoading = false;
		} else {
			firstLoading = true;
		}
	}

	public void onBeforeBrowseNotify() {
		notifySubscribers(EventType.onNavigationStart);
	}

	public void onAddressChangeNotify(boolean isLoading, String url) {
		Map<String, Object> mapData = new HashMap<>();
		mapData.put("errorCode", _errorCode);
		_errorCode = 0;
		notifySubscribers(EventType.onNavigationFinished, mapData);
	}

	public void onLoadErrorNotify(int error) {
		Map<String, Object> mapData = new HashMap<>();
		mapData.put("errorCode", error);
		_errorCode = error;
		notifySubscribers(EventType.onLoadError, mapData);
	}

	public void onLoadStartNotify() {
		notifySubscribers(EventType.onLoadStart);
	}

	public void onFindResultNotify(int count, int activeMatchOrdinal) {
		Map<String, Object> mapData = new HashMap<>();
		mapData.put("count", count);
		mapData.put("activeMatchOrdinal", activeMatchOrdinal);
		notifySubscribers(EventType.onFindResult, mapData);
	}

	public void onConsoleMessageNotify(LogSeverity level, String message, String source, int line) {
		Map<String, Object> mapData = new HashMap<>();
		mapData.put("level", level.toString());
		mapData.put("message", message);
		mapData.put("source", source);
		mapData.put("line", line);
		notifySubscribers(EventType.onConsoleMessage, mapData);
	}

	public void onFullscreenModeChangeNotify(boolean fullscreen) {
		notifySubscribers(fullscreen ? EventType.onFullScreenEntered : EventType.onFullScreenExited);
	}
}
