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
/// Implement this structure to handle events related to JavaScript dialogs. The
/// functions of this structure will be called on the UI thread.
///
public class cef_jsdialog_handler_t {
  ///
  /// Base structure.
  ///
  public cef_base_ref_counted_t base;
  ///
  /// Called to run a JavaScript dialog. If |origin_url| is non-NULL it can be
  /// passed to the CefFormatUrlForSecurityDisplay function to retrieve a secure
  /// and user-friendly display string. The |default_prompt_text| value will be
  /// specified for prompt dialogs only. Set |suppress_message| to true (1) and
  /// return false (0) to suppress the message (suppressing messages is
  /// preferable to immediately executing the callback as this is used to detect
  /// presumably malicious behavior like spamming alert messages in
  /// onbeforeunload). Set |suppress_message| to false (0) and return false (0)
  /// to use the default implementation (the default implementation will show one
  /// modal dialog at a time and suppress any additional dialog requests until
  /// the displayed dialog is dismissed). Return true (1) if the application will
  /// use a custom dialog or if the callback has been executed immediately.
  /// Custom dialogs may be either modal or modeless. If a custom dialog is used
  /// the application must execute |callback| once the custom dialog is
  /// dismissed.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_jsdialog;
  ///
  /// Called to run a dialog asking the user if they want to leave a page. Return
  /// false (0) to use the default dialog implementation. Return true (1) if the
  /// application will use a custom dialog or if the callback has been executed
  /// immediately. Custom dialogs may be either modal or modeless. If a custom
  /// dialog is used the application must execute |callback| once the custom
  /// dialog is dismissed.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_before_unload_dialog;
  ///
  /// Called to cancel any pending dialogs and reset any saved dialog state. Will
  /// be called due to events like page navigation irregardless of whether any
  /// dialogs are currently pending.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_reset_dialog_state;
  ///
  /// Called when the default implementation dialog is closed.
  ///
  /** @field cast=(void*) */
  public long /* int */ on_dialog_closed;

  /** @field flags=no_gen */
  public long /* int */ ptr;
  /** @field flags=no_gen */
  public Callback on_jsdialog_cb;

  public static final int sizeof = ChromiumLib.cef_jsdialog_handler_t_sizeof();

  public static final int JSDIALOGTYPE_ALERT = 0;
  public static final int JSDIALOGTYPE_CONFIRM = 1;
  public static final int JSDIALOGTYPE_PROMPT = 2;

}