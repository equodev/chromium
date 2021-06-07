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
/// Structure used to implement browser process callbacks. The functions of this
/// structure will be called on the browser process main thread unless otherwise
/// indicated.
///
public class cef_browser_process_handler_t {
  ///
  /// Base structure.
  ///
  public cef_base_ref_counted_t base;
  ///
  /// Called on the browser process UI thread immediately after the CEF context
  /// has been initialized.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_context_initialized;
  ///
  /// Called before a child process is launched. Will be called on the browser
  /// process UI thread when launching a render process and on the browser
  /// process IO thread when launching a GPU or plugin process. Provides an
  /// opportunity to modify the child process command line. Do not keep a
  /// reference to |command_line| outside of this function.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_before_child_process_launch;
  ///
  /// Called on the browser process IO thread after the main thread has been
  /// created for a new render process. Provides an opportunity to specify extra
  /// information that will be passed to
  /// cef_render_process_handler_t::on_render_thread_created() in the render
  /// process. Do not keep a reference to |extra_info| outside of this function.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_render_process_thread_created;
  ///
  /// Return the handler for printing on Linux. If a print handler is not
  /// provided then printing will not be supported on the Linux platform.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_print_handler;
  ///
  /// Called from any thread when work has been scheduled for the browser process
  /// main (UI) thread. This callback is used in combination with CefSettings.
  /// external_message_pump and cef_do_message_loop_work() in cases where the CEF
  /// message loop must be integrated into an existing application message loop
  /// (see additional comments and warnings on CefDoMessageLoopWork). This
  /// callback should schedule a cef_do_message_loop_work() call to happen on the
  /// main (UI) thread. |delay_ms| is the requested delay in milliseconds. If
  /// |delay_ms| is <= 0 then the call should happen reasonably soon. If
  /// |delay_ms| is > 0 then the call should be scheduled to happen after the
  /// specified delay and any currently pending scheduled call should be
  /// cancelled.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_schedule_message_pump_work;

  /** @field flags=no_gen */
  public long /* int */ ptr;
  /** @field flags=no_gen */
  public Callback on_schedule_message_pump_work_cb;

  public static final int sizeof = ChromiumLib.cef_browser_process_handler_t_sizeof();

}
