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

///
/// Implement this structure to handle events related to keyboard input. The
/// functions of this structure will be called on the UI thread.
///
public class cef_keyboard_handler_t {
  ///
  /// Base structure.
  ///
  public cef_base_ref_counted_t base;
  ///
  /// Called before a keyboard event is sent to the renderer. |event| contains
  /// information about the keyboard event. |os_event| is the operating system
  /// event message, if any. Return true (1) if the event was handled or false
  /// (0) otherwise. If the event will be handled in on_key_event() as a keyboard
  /// shortcut set |is_keyboard_shortcut| to true (1) and return false (0).
  ///
  /** @field cast=(void*) */
  public long /* int */ on_pre_key_event;
  ///
  /// Called after the renderer and JavaScript in the page has had a chance to
  /// handle the event. |event| contains information about the keyboard event.
  /// |os_event| is the operating system event message, if any. Return true (1)
  /// if the keyboard event was handled or false (0) otherwise.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_key_event;

  /** @field flags=no_gen */
  public long /* int */ ptr;
  /** @field flags=no_gen */
  public Callback on_key_event_cb;

  public static final int sizeof = ChromiumLib.cef_display_handler_t_sizeof();
}
