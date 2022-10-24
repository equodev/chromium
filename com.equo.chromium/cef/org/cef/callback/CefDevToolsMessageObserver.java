/****************************************************************************
**
** Copyright (C) 2022 Equo
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


package org.cef.callback;

import org.cef.browser.CefBrowser;

public interface CefDevToolsMessageObserver {

	/**
	 * Method that will be called on receipt of a DevTools protocol message.
	 * |browser| is the originating browser instance. |message| is a UTF8-encoded
	 * JSON dictionary representing either a method result or an event. |message| is
	 * only valid for the scope of this callback and should be copied if necessary.
	 *
	 * Method result dictionaries include an "id" (int) value that identifies the
	 * orginating method call sent from CefBrowserHost::SendDevToolsMessage, and
	 * optionally either a "result" (dictionary) or "error" (dictionary) value. The
	 * "error" dictionary will contain "code" (int) and "message" (string) values.
	 * Event dictionaries include a "method" (string) value and optionally a
	 * "params" (dictionary) value. See the DevTools protocol documentation at
	 * https:*chromedevtools.github.io/devtools-protocol/ for details of supported
	 * method calls and the expected "result" or "params" dictionary contents. JSON
	 * dictionaries can be parsed using the CefParseJSON function if desired,
	 * however be aware of performance considerations when parsing large messages
	 * (some of which may exceed 1MB in size).
	 * 
	 * @param cefBrowser
	 * @param message
	 * @param messageSize
	 * @return true if the message was handled or false if the message should be
	 *         further processed and passed to the OnDevToolsMethodResult or
	 *         OnDevToolsEvent methods as appropriate.
	 */
	public boolean onDevToolsMessage(CefBrowser cefBrowser, String message, int messageSize);

	/**
	 * Method that will be called after attempted execution of a DevTools protocol
	 * method. |browser| is the originating browser instance. |message_id| is the
	 * "id" value that identifies the originating method call message. If the method
	 * succeeded |success| will be true and |result| will be the UTF8-encoded JSON
	 * "result" dictionary value (which may be empty). If the method failed
	 * |success| will be false and |result| will be the UTF8-encoded JSON "error"
	 * dictionary value. |result| is only valid for the scope of this callback and
	 * should be copied if necessary. See the OnDevToolsMessage documentation for
	 * additional details on |result| contents.
	 * 
	 * @param cefBrowser
	 * @param messageId
	 * @param success
	 * @param result
	 * @param resultSize
	 */
	public void onDevToolsMethodResult(CefBrowser cefBrowser, int messageId, boolean success, String result,
			int resultSize);

	/**
	 * Method that will be called on receipt of a DevTools protocol event. |browser|
	 * is the originating browser instance. |method| is the "method" value. |params|
	 * is the UTF8-encoded JSON "params" dictionary value (which may be empty).
	 * |params| is only valid for the scope of this callback and should be copied if
	 * necessary. See the OnDevToolsMessage documentation for additional details on
	 * |params| contents.
	 * 
	 * @param cefBrowser
	 * @param method
	 * @param params
	 * @param paramsSize
	 */
	public void onDevToolsEvent(CefBrowser cefBrowser, String method, String params, int paramsSize);

	/**
	 * Method that will be called when the DevTools agent has attached. |browser| is
	 * the originating browser instance. This will generally occur in response to
	 * the first message sent while the agent is detached.
	 * 
	 * @param cefBrowser
	 */
	public void onDevToolsAgentAttached(CefBrowser cefBrowser);

	/**
	 * Method that will be called when the DevTools agent has detached. |browser| is
	 * the originating browser instance. Any method results that were pending before
	 * the agent became detached will not be delivered, and any active event
	 * subscriptions will be canceled.
	 * 
	 * @param cefBrowser
	 */
	public void onDevToolsAgentDetached(CefBrowser cefBrowser);
}
