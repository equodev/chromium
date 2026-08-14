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

import org.cef.CefApp;

/**
 * Defines the API available for Standalone browsers.
 *
 * <p>
 * This interface extends {@link ChromiumBrowser} and exposes additional
 * capabilities that are specific to browsers running in Standalone mode.
 * These features apply to the native window associated with the browser and
 * are not applicable when embedding Chromium into SWT, Swing or Windowless
 * environments.
 * </p>
 *
 * <p>
 * Unless otherwise specified by a particular method, all operations defined by
 * this interface may be invoked from any thread.
 * </p>
 */
public interface ChromiumBrowserStandalone extends ChromiumBrowser {

	/**
	 * Terminates the CEF message loop and shuts down the global CefApp instance.
	 *
	 * <p>Behavior depends on the {@code chromium.manual_stop_browsers} setting:</p>
	 * <ul>
	 *   <li><b>true (default)</b>: CEF will <b>not</b> stop the message loop automatically when the last browser is
	 *       closed. The application is responsible for deciding <i>when</i> to terminate CEF. In that
	 *       mode you must call this method (or an equivalent shutdown path) to release native resources
	 *       and ensure helper processes (zygote/gpu/utility, etc.) exit even if the JVM remains alive.</li>
	 *   <li><b>false</b>: CEF may stop the message loop automatically as part of the shutdown
	 *       sequence when the last browser/client is closed. This method is called automatically.</li>
	 * </ul>
	 *
	 * <p><b>Important:</b> This shuts down the global CEF context. After calling it, existing browsers
	 * are no longer usable and creating new browsers in the same process is not supported unless the
	 * embedding explicitly supports re-initialization.</p>
	 *
	 * @since 128.0.33
	 */
	public static void stopBrowsers() {
		CefApp app = null;
		try {
			app = CefApp.getInstance();
		} catch (IllegalStateException e) {
			return;
		}
		if (!Boolean.getBoolean("chromium.multi_threaded_message_loop")) {
			app.shutdown();
		} else {
			app.quitMessageLoop();
			app.shutdown();
		}
	}

	/**
	 * Resizes the browser window.
	 *
	 * @param width  the new width of the window, in pixels.
	 * @param height the new height of the window, in pixels.
	 *
	 * @since 128.0.27
	 */
	void setWindowSize(int width, int height);

	/**
	 * Sets the position and size of the browser window.
	 * <p>
	 * Supported on Standalone browsers on all operating systems except Wayland (x and y
	 * parameters may be ignored and only the window size will be applied).
	 * </p>
	 *
	 * @param x      the new x-coordinate of the window, in pixels.
	 * @param y      the new y-coordinate of the window, in pixels.
	 * @param width  the new width of the window, in pixels.
	 * @param height the new height of the window, in pixels.
	 *
	 * @since 128.0.28
	 */
	public void setWindowBounds(int x, int y, int width, int height);

	/**
	 * Sets the position of the browser window.
	 * <p>
	 * Supported on Standalone browsers on all operating systems except Wayland. On
	 * Wayland, the compositor may ignore the requested position.
	 * </p>
	 *
	 * @param x the new x-coordinate of the window, in pixels.
	 * @param y the new y-coordinate of the window, in pixels.
	 *
	 * @since 128.0.29
	 */
	void setWindowPosition(int x, int y);

	/**
	 * Maximizes the browser window.
	 *
	 * @since 128.0.28
	 */
	void maximizeWindow();

	/**
	 * Minimizes the browser window.
	 *
	 * @since 128.0.28
	 */
	void minimizeWindow();

	/**
	 * Minimizes the browser window.
	 *
	 * @since 128.0.35
	 */
	void restoreWindow();
}
