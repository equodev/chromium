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

package com.equo.swt.internal.chromium.lib;

import org.eclipse.swt.internal.Callback;

import com.equo.swt.internal.chromium.lib.ChromiumLib;
import com.equo.swt.internal.chromium.lib.cef_base_ref_counted_t;

///
/// Implement this structure to provide handler implementations. Methods will be
/// called by the process and/or thread indicated.
///
public class cef_app_t {
  ///
  /// Base structure.
  ///
  public cef_base_ref_counted_t base;
  ///
  /// Provides an opportunity to view and/or modify command-line arguments before
  /// processing by CEF and Chromium. The |process_type| value will be NULL for
  /// the browser process. Do not keep a reference to the cef_command_line_t
  /// object passed to this function. The CefSettings.command_line_args_disabled
  /// value can be used to start with an NULL command-line object. Any values
  /// specified in CefSettings that equate to command-line arguments will be set
  /// before this function is called. Be cautious when using this function to
  /// modify command-line arguments for non-browser processes as this may result
  /// in undefined behavior including crashes.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_before_command_line_processing;
  ///
  /// Provides an opportunity to register custom schemes. Do not keep a reference
  /// to the |registrar| object. This function is called on the main thread for
  /// each process and the registered schemes should be the same across all
  /// processes.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_register_custom_schemes;
  ///
  /// Return the handler for resource bundle events. If
  /// CefSettings.pack_loading_disabled is true (1) a handler must be returned.
  /// If no handler is returned resources will be loaded from pack files. This
  /// function is called by the browser and render processes on multiple threads.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_resource_bundle_handler;
  ///
  /// Return the handler for functionality specific to the browser process. This
  /// function is called on multiple threads in the browser process.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_browser_process_handler;
  ///
  /// Return the handler for functionality specific to the render process. This
  /// function is called on the render process main thread.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_render_process_handler;

  /** @field flags=no_gen */
  public long /* int */ ptr;
  /** @field flags=no_gen */
  public Callback get_browser_process_handler_cb;

  public static final int sizeof = ChromiumLib.cef_app_t_sizeof();
}