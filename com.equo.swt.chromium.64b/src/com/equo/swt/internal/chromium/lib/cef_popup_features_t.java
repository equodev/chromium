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

import com.equo.swt.internal.chromium.lib.ChromiumLib;

///
/// Popup window features.
///
public class cef_popup_features_t {
  public int x;
  public int xSet;
  public int y;
  public int ySet;
  public int width;
  public int widthSet;
  public int height;
  public int heightSet;
  public int menuBarVisible;
  public int statusBarVisible;
  public int toolBarVisible;
  public int scrollbarsVisible;

  public static final int sizeof = ChromiumLib.cef_popup_features_t_sizeof();
}