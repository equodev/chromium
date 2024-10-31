// Copyright (c) 2024 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

/**
 * Used internally by {@link CefDevToolsClient}.
 * <p>
 * Callback interface for {@link CefBrowser#addDevToolsMessageObserver(CefDevToolsMessageObserver)}.
 * The methods of this class will be called on the CEF UI thread.
 */
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
     * Method that will be called after attempted execution of a DevTools protocol method.
     *
     * @param browser the originating browser instance
     * @param messageId the ID that identifies the originating method call
     * @param success if the method succeeded, |success| will be true and |result| will be a JSON
     *        object containing the method call result. If the method call failed, |success| will
     *        be false and |result| will be a JSON object describing the error.
     * @param result method call result or an error
     * @param resultSize
     */
    public void onDevToolsMethodResult(CefBrowser browser, int messageId, boolean success, String result,
                    int resultSize);

    /**
     * Method that will be called on receipt of a DevTools protocol event.
     *
     * @param browser the originating browser instance
     * @param method the method name
     * @param parameters the event data
     * @param paramsSize
     */
    public void onDevToolsEvent(CefBrowser browser, String method, String parameters, int paramsSize);

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
