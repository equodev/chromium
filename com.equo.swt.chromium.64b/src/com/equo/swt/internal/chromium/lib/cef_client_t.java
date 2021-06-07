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
/// Implement this structure to provide handler implementations.
///
public class cef_client_t {
  ///
  /// Base structure.
  ///
  public cef_base_ref_counted_t base;
  ///
  /// Return the handler for context menus. If no handler is provided the default
  /// implementation will be used.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_context_menu_handler;
  ///
  /// Return the handler for dialogs. If no handler is provided the default
  /// implementation will be used.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_dialog_handler;
  ///
  /// Return the handler for browser display state events.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_display_handler;
  ///
  /// Return the handler for download events. If no handler is returned downloads
  /// will not be allowed.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_download_handler;
  ///
  /// Return the handler for drag events.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_drag_handler;
  ///
  /// Return the handler for find result events.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_find_handler;
  ///
  /// Return the handler for focus events.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_focus_handler;
  ///
  /// Return the handler for JavaScript dialogs. If no handler is provided the
  /// default implementation will be used.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_jsdialog_handler;
  ///
  /// Return the handler for keyboard events.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_keyboard_handler;
  ///
  /// Return the handler for browser life span events.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_life_span_handler;
  ///
  /// Return the handler for browser load status events.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_load_handler;
  ///
  /// Return the handler for off-screen rendering events.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_render_handler;
  ///
  /// Return the handler for browser request events.
  ///
  /** @field cast=(void*) */
  public long /* int */ get_request_handler;
  ///
  /// Called when a new message is received from a different process. Return true
  /// (1) if the message was handled or false (0) otherwise. Do not keep a
  /// reference to or attempt to access the message outside of this callback.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_process_message_received;

  /** @field flags=no_gen */
  public long /* int */ ptr;
  /** @field flags=no_gen */
  public Callback get_life_span_handler_cb;
  /** @field flags=no_gen */
  public Callback get_context_menu_handler_cb;
  /** @field flags=no_gen */
  public Callback on_process_message_received_cb;
  /** @field flags=no_gen */
  public Callback get_jsdialog_handler_cb;
  /** @field flags=no_gen */
  public Callback get_load_handler_cb;
  /** @field flags=no_gen */
  public Callback get_display_handler_cb;
  /** @field flags=no_gen */
  public Callback get_request_handler_cb;
  /** @field flags=no_gen */
  public Callback get_focus_handler_cb;
  /** @field flags=no_gen */
  public Callback get_keyboard_handler_cb;

  public static final int sizeof = ChromiumLib.cef_client_t_sizeof();

}