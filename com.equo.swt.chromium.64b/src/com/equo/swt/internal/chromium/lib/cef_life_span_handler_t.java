/****************************************************************************
**
** Copyright (C) 2021 Equo
**
** This file is part of Equo Chromium.
**
** Commercial License Usage
** Licensees holding valid commercial Equo licenses may use this file in
** accordance with the commercial license agreement provided with the
** Software or, alternatively, in accordance with the terms contained in
** a written agreement between you and Equo. For licensing terms
** and conditions see https://www.equoplatform.com/terms.
**
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3 as published by the Free Software
** Foundation. Please review the following
** information to ensure the GNU General Public License requirements will
** be met: https://www.gnu.org/licenses/gpl-3.0.html.
**
****************************************************************************/

package com.equo.swt.internal.chromium.lib;

import org.eclipse.swt.internal.Callback;

import com.equo.swt.internal.chromium.lib.ChromiumLib;
import com.equo.swt.internal.chromium.lib.cef_base_ref_counted_t;

///
/// Implement this structure to handle events related to browser life span. The
/// functions of this structure will be called on the UI thread unless otherwise
/// indicated.
///
public class cef_life_span_handler_t {
  ///
  /// Base structure.
  ///
  public cef_base_ref_counted_t base;
  ///
  /// Called on the IO thread before a new popup browser is created. The
  /// |browser| and |frame| values represent the source of the popup request. The
  /// |target_url| and |target_frame_name| values indicate where the popup
  /// browser should navigate and may be NULL if not specified with the request.
  /// The |target_disposition| value indicates where the user intended to open
  /// the popup (e.g. current tab, new tab, etc). The |user_gesture| value will
  /// be true (1) if the popup was opened via explicit user gesture (e.g.
  /// clicking a link) or false (0) if the popup opened automatically (e.g. via
  /// the DomContentLoaded event). The |popupFeatures| structure contains
  /// additional information about the requested popup window. To allow creation
  /// of the popup browser optionally modify |windowInfo|, |client|, |settings|
  /// and |no_javascript_access| and return false (0). To cancel creation of the
  /// popup browser return true (1). The |client| and |settings| values will
  /// default to the source browser's values. If the |no_javascript_access| value
  /// is set to false (0) the new browser will not be scriptable and may not be
  /// hosted in the same renderer process as the source browser. Any
  /// modifications to |windowInfo| will be ignored if the parent browser is
  /// wrapped in a cef_browser_view_t. Popup browser creation will be canceled if
  /// the parent browser is destroyed before the popup browser creation completes
  /// (indicated by a call to OnAfterCreated for the popup browser).
  ///
  /** @field cast=(void*) */
  public long /* int */ on_before_popup;
  ///
  /// Called after a new browser is created. This callback will be the first
  /// notification that references |browser|.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_after_created;
  ///
  /// Called when a browser has recieved a request to close. This may result
  /// directly from a call to cef_browser_host_t::*close_browser() or indirectly
  /// if the browser is parented to a top-level window created by CEF and the
  /// user attempts to close that window (by clicking the 'X', for example). The
  /// do_close() function will be called after the JavaScript 'onunload' event
  /// has been fired.
  ///
  /// An application should handle top-level owner window close notifications by
  /// calling cef_browser_host_t::try_close_browser() or
  /// cef_browser_host_t::CloseBrowser(false (0)) instead of allowing the window
  /// to close immediately (see the examples below). This gives CEF an
  /// opportunity to process the 'onbeforeunload' event and optionally cancel the
  /// close before do_close() is called.
  ///
  /// When windowed rendering is enabled CEF will internally create a window or
  /// view to host the browser. In that case returning false (0) from do_close()
  /// will send the standard close notification to the browser's top-level owner
  /// window (e.g. WM_CLOSE on Windows, performClose: on OS X, "delete_event" on
  /// Linux or cef_window_delegate_t::can_close() callback from Views). If the
  /// browser's host window/view has already been destroyed (via view hierarchy
  /// tear-down, for example) then do_close() will not be called for that browser
  /// since is no longer possible to cancel the close.
  ///
  /// When windowed rendering is disabled returning false (0) from do_close()
  /// will cause the browser object to be destroyed immediately.
  ///
  /// If the browser's top-level owner window requires a non-standard close
  /// notification then send that notification from do_close() and return true
  /// (1).
  ///
  /// The cef_life_span_handler_t::on_before_close() function will be called
  /// after do_close() (if do_close() is called) and immediately before the
  /// browser object is destroyed. The application should only exit after
  /// on_before_close() has been called for all existing browsers.
  ///
  /// The below examples describe what should happen during window close when the
  /// browser is parented to an application-provided top-level window.
  ///
  /// Example 1: Using cef_browser_host_t::try_close_browser(). This is
  /// recommended for clients using standard close handling and windows created
  /// on the browser process UI thread. 1. User clicks the window close button
  /// which sends a close notification to
  /// the application's top-level window.
  /// 2. Application's top-level window receives the close notification and
  /// calls TryCloseBrowser() (which internally calls CloseBrowser(false)).
  /// TryCloseBrowser() returns false so the client cancels the window close.
  /// 3. JavaScript 'onbeforeunload' handler executes and shows the close
  /// confirmation dialog (which can be overridden via
  /// CefJSDialogHandler::OnBeforeUnloadDialog()).
  /// 4. User approves the close. 5. JavaScript 'onunload' handler executes. 6.
  /// CEF sends a close notification to the application's top-level window
  /// (because DoClose() returned false by default).
  /// 7. Application's top-level window receives the close notification and
  /// calls TryCloseBrowser(). TryCloseBrowser() returns true so the client
  /// allows the window close.
  /// 8. Application's top-level window is destroyed. 9. Application's
  /// on_before_close() handler is called and the browser object
  /// is destroyed.
  /// 10. Application exits by calling cef_quit_message_loop() if no other
  /// browsers
  /// exist.
  ///
  /// Example 2: Using cef_browser_host_t::CloseBrowser(false (0)) and
  /// implementing the do_close() callback. This is recommended for clients using
  /// non-standard close handling or windows that were not created on the browser
  /// process UI thread. 1. User clicks the window close button which sends a
  /// close notification to
  /// the application's top-level window.
  /// 2. Application's top-level window receives the close notification and:
  /// A. Calls CefBrowserHost::CloseBrowser(false).
  /// B. Cancels the window close.
  /// 3. JavaScript 'onbeforeunload' handler executes and shows the close
  /// confirmation dialog (which can be overridden via
  /// CefJSDialogHandler::OnBeforeUnloadDialog()).
  /// 4. User approves the close. 5. JavaScript 'onunload' handler executes. 6.
  /// Application's do_close() handler is called. Application will:
  /// A. Set a flag to indicate that the next close attempt will be allowed.
  /// B. Return false.
  /// 7. CEF sends an close notification to the application's top-level window.
  /// 8. Application's top-level window receives the close notification and
  /// allows the window to close based on the flag from #6B.
  /// 9. Application's top-level window is destroyed. 10. Application's
  /// on_before_close() handler is called and the browser object
  /// is destroyed.
  /// 11. Application exits by calling cef_quit_message_loop() if no other
  /// browsers
  /// exist.
  ///
  /** @field cast=(void*) */
  public long /* int */ do_close;
  ///
  /// Called just before a browser is destroyed. Release all references to the
  /// browser object and do not attempt to execute any functions on the browser
  /// object after this callback returns. This callback will be the last
  /// notification that references |browser|. See do_close() documentation for
  /// additional usage information.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_before_close;

  /** @field flags=no_gen */
  public long /* int */ ptr;
  /** @field flags=no_gen */
  public Callback on_before_popup_cb;
  /** @field flags=no_gen */
  public Callback on_after_created_cb;
  /** @field flags=no_gen */
  public Callback do_close_cb;
  /** @field flags=no_gen */
  public Callback on_before_close_cb;

  public static final int sizeof = ChromiumLib.cef_life_span_handler_t_sizeof();

}