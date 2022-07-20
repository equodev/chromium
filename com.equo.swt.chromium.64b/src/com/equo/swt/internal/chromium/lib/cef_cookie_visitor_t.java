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
/// Structure to implement for visiting cookie values. The functions of this
/// structure will always be called on the IO thread.
///
public class cef_cookie_visitor_t {
  ///
  /// Base structure.
  ///
  public cef_base_ref_counted_t base;
  ///
  /// Method that will be called once for each cookie. |count| is the 0-based
  /// index for the current cookie. |total| is the total number of cookies. Set
  /// |deleteCookie| to true (1) to delete the cookie currently being visited.
  /// Return false (0) to stop visiting cookies. This function may never be
  /// called if no cookies are found.
  ///
  /** @field cast=(void*) */
  public long /* int */ visit;

  /** @field flags=no_gen */
  public long /* int */ ptr;
  /** @field flags=no_gen */
  public Callback visit_cb;

  public static final int sizeof = ChromiumLib.cef_cookie_visitor_t_sizeof();
}