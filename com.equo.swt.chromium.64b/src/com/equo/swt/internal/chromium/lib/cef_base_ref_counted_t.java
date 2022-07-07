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

///
/// All ref-counted framework structures must include this structure first.
///
public class cef_base_ref_counted_t {
  ///
  /// Size of the data structure.
  ///
  /** @field cast=(size_t) */
  public int size;
  ///
  /// Called to increment the reference count for the object. Should be
  /// called
  /// for every new copy of a pointer to a given object.
  ///
  /** @field cast=(void*) */
  public long /* int */ add_ref;
  ///
  /// Called to decrement the reference count for the object. If the
  /// reference
  /// count falls to 0 the object should self-delete. Returns true (1) if
  /// the
  /// resulting reference count is 0.
  ///
  /** @field cast=(void*) */
  public long /* int */ release;
  ///
  /// Returns true (1) if the current reference count is 1.
  ///
  /** @field cast=(void*) */
  public long /* int */ has_one_ref;

}