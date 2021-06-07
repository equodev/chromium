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
/// Implement this structure to handle events related to browser display state.
/// The functions of this structure will be called on the UI thread.
///
public class cef_display_handler_t {
  ///
  /// Base structure.
  ///
  public cef_base_ref_counted_t base;
  ///
  /// Called when a frame's address has changed.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_address_change;
  ///
  /// Called when the page title changes.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_title_change;
  ///
  /// Called when the page icon changes.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_favicon_urlchange;
  ///
  /// Called when web content in the page has toggled fullscreen mode. If
  /// |fullscreen| is true (1) the content will automatically be sized to fill
  /// the browser content area. If |fullscreen| is false (0) the content will
  /// automatically return to its original size and position. The client is
  /// responsible for resizing the browser if desired.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_fullscreen_mode_change;
  ///
  /// Called when the browser is about to display a tooltip. |text| contains the
  /// text that will be displayed in the tooltip. To handle the display of the
  /// tooltip yourself return true (1). Otherwise, you can optionally modify
  /// |text| and then return false (0) to allow the browser to display the
  /// tooltip. When window rendering is disabled the application is responsible
  /// for drawing tooltips and the return value is ignored.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_tooltip;
  ///
  /// Called when the browser receives a status message. |value| contains the
  /// text that will be displayed in the status message.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_status_message;
  ///
  /// Called to display a console message. Return true (1) to stop the message
  /// from being output to the console.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_console_message;
  ///
  /// Called when auto-resize is enabled via
  /// cef_browser_host_t::SetAutoResizeEnabled and the contents have auto-
  /// resized. |new_size| will be the desired size in view coordinates. Return
  /// true (1) if the resize was handled or false (0) for default handling.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_auto_resize;
  ///
  /// Called when auto-resize is enabled via
  /// cef_browser_host_t::SetAutoResizeEnabled and the contents have auto-
  /// resized. |new_size| will be the desired size in view coordinates. Return
  /// true (1) if the resize was handled or false (0) for default handling.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_loading_progress_change;

  /** @field flags=no_gen */
  public long /* int */ ptr;
  /** @field flags=no_gen */
  public Callback on_title_change_cb;
  /** @field flags=no_gen */
  public Callback on_address_change_cb;
  /** @field flags=no_gen */
  public Callback on_status_message_cb;

  public static final int sizeof = ChromiumLib.cef_display_handler_t_sizeof();
}
