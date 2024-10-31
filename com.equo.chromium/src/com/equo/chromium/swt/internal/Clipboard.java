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
package com.equo.chromium.swt.internal;

import static com.equo.chromium.swt.internal.Chromium.asyncExec;

import java.util.ArrayList;
import java.util.List;

import org.cef.CefClientSwt;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

public class Clipboard {

	public static void createClipboardRouters(CefClientSwt clientHandler) {
		CefMessageRouter writeText = CefMessageRouter
				.create(new CefMessageRouterConfig("__writeText", "__writeTextCancel"));
		CefMessageRouter readText = CefMessageRouter
				.create(new CefMessageRouterConfig("__readText", "__readTextCancel"));
		CefMessageRouter write = CefMessageRouter.create(new CefMessageRouterConfig("__write", "__writeCancel"));
		CefMessageRouter read = CefMessageRouter.create(new CefMessageRouterConfig("__read", "__readCancel"));

		CefMessageRouterHandlerAdapter writeTextHandler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {
				Display display = Display.getDefault();
				asyncExec(() -> {
					final org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(display);
					try {
						if (request == null || request.isEmpty()) {
							cb.clearContents();
						} else {
							Transfer transfer = TextTransfer.getInstance();
							cb.setContents(new Object[] { request }, new Transfer[] { transfer });
						}
						callback.success(request);
					} catch (Exception e) {
						e.printStackTrace();
						callback.failure(0, e.getMessage());
					}
					cb.dispose();
				});
				return true;
			}
		};
		CefMessageRouterHandlerAdapter readTextHandler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {
				Display display = Display.getDefault();
				asyncExec(() -> {
					Transfer[] transfers = { TextTransfer.getInstance(), HTMLTransfer.getInstance() };
					final org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(display);
					for (int i = 0; i < transfers.length; i++) {
						try {
							String text = (String) cb.getContents(transfers[i]);
							if (text != null) {
								callback.success(text);
							}
						} catch (Exception e) {
							e.printStackTrace();
							callback.failure(0, e.getMessage());
						}
					}
					cb.dispose();
				});
				return true;
			}
		};
		CefMessageRouterHandlerAdapter writeHandler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {
				Display display = Display.getDefault();
				asyncExec(() -> {
					final org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(display);
					try {
						if (request == null || request.isEmpty()) {
							cb.clearContents();
						} else {
							JsonArray jsonArr = (JsonArray) Jsoner.deserialize(request);
							List<Transfer> transferSet = new ArrayList<>();
							List<Object> dataSet = new ArrayList<>();
							for (Object blobObject : jsonArr) {
								JsonObject jsonObj = (JsonObject) blobObject;
								String mimeType = (String) jsonObj.keySet().iterator().next();
								Object data = jsonObj.get(mimeType);
								Transfer transfer = null;
								switch (mimeType) {
								case "text/plain":
									transfer = TextTransfer.getInstance();
									break;
								case "text/html":
									transfer = HTMLTransfer.getInstance();
									break;
								default:
									System.out.println("Copy type " + mimeType + " is not supported");
								}
								if (data != null) {
									dataSet.add(data);
									transferSet.add(transfer);
								}
							}
							if (!dataSet.isEmpty()) {
								cb.setContents(dataSet.toArray(new String[dataSet.size()]),
										transferSet.toArray(new Transfer[transferSet.size()]));
							}
						}
						callback.success(request);
					} catch (Exception e) {
						e.printStackTrace();
						callback.failure(0, e.getMessage());
					}
					cb.dispose();
				});
				return true;
			}
		};
		CefMessageRouterHandlerAdapter readHandler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {
				Display display = Display.getDefault();
				asyncExec(() -> {
					final org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(display);
					Transfer[] transfers = { TextTransfer.getInstance(), HTMLTransfer.getInstance() };

					JsonArray jsonArray = new JsonArray();
					for (int i = 0; i < transfers.length; i++) {
						try {
							Object data = cb.getContents(transfers[i]);
							if (data != null) {
								String type = "";
								if (transfers[i] instanceof TextTransfer) {
									type = "text/plain";
								} else if (transfers[i] instanceof HTMLTransfer) {
									type = "text/html";
								}
								JsonObject jsonObject = new JsonObject();
								jsonObject.put("type", type);
								jsonObject.put("data", data);
								jsonArray.add(jsonObject);
							}
						} catch (Exception e) {
							e.printStackTrace();
							callback.failure(0, e.getMessage());
						}
					}
					callback.success(Jsoner.serialize(jsonArray));
					cb.dispose();
				});
				return true;
			}
		};
		writeText.addHandler(writeTextHandler, true);
		readText.addHandler(readTextHandler, true);
		write.addHandler(writeHandler, true);
		read.addHandler(readHandler, true);
		clientHandler.addMessageRouter(writeText);
		clientHandler.addMessageRouter(readText);
		clientHandler.addMessageRouter(write);
		clientHandler.addMessageRouter(read);
	};
}
