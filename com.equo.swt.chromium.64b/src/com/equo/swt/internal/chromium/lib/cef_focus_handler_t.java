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
/// Implement this structure to handle events related to focus. The functions of
/// this structure will be called on the UI thread.
///
public class cef_focus_handler_t {
  ///
  /// Base structure.
  ///
  public cef_base_ref_counted_t base;
  ///
  /// Called when the browser component is about to loose focus. For instance, if
  /// focus was on the last HTML element and the user pressed the TAB key. |next|
  /// will be true (1) if the browser is giving focus to the next component and
  /// false (0) if the browser is giving focus to the previous component.
  ///
  /** 
   * @field cast=(void*)
   * */
  public long /* int */ on_take_focus;
  ///
  /// Called when the browser component is requesting focus. |source| indicates
  /// where the focus request is originating from. Return false (0) to allow the
  /// focus to be set or true (1) to cancel setting the focus.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_set_focus;
  ///
  /// Called when the browser component has received focus.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_got_focus;

  /** @field flags=no_gen */
  public long /* int */ ptr;
  /** @field flags=no_gen */
  public Callback on_got_focus_cb;
  /** @field flags=no_gen */
  public Callback on_set_focus_cb;
  /** @field flags=no_gen */
  public Callback on_take_focus_cb;

  public static final int sizeof = ChromiumLib.cef_focus_handler_t_sizeof();

}