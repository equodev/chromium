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

///
/// Structure representing keyboard event information.
///
public class cef_key_event_t {
  ///
  /// The type of keyboard event.
  ///
  public int type;
  ///
  /// Bit flags describing any pressed modifier keys. See
  /// cef_event_flags_t for values.
  ///
  public int modifiers;
  ///
  /// The Windows key code for the key event. This value is used by the DOM
  /// specification. Sometimes it comes directly from the event (i.e. on
  /// Windows) and sometimes it's determined using a mapping function. See
  /// WebCore/platform/chromium/KeyboardCodes.h for the list of values.
  ///
  public int windows_key_code;
  ///
  /// The actual key code genenerated by the platform.
  ///
  public int native_key_code;
  ///
  /// Indicates whether the event is considered a "system key" event (see
  /// http://msdn.microsoft.com/en-us/library/ms646286(VS.85).aspx for details).
  /// This value will always be false on non-Windows platforms.
  ///
  public int is_system_key;
  ///
  /// The character generated by the keystroke.
  ///
  public char character;
  ///
  /// Same as |character| but unmodified by any concurrently-held modifiers
  /// (except shift). This is useful for working out shortcut keys.
  ///
  public char unmodified_character;
  ///
  /// True if the focus is currently on an editable field on the page. This is
  /// useful for determining if standard key events should be intercepted.
  ///
  public int focus_on_editable_field;

  public static final int sizeof = ChromiumLib.cef_key_event_t_sizeof();
}