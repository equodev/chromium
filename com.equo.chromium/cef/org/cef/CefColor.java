/****************************************************************************
**
** Copyright (C) 2026 Equo
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
package org.cef;

public class CefColor {

	public static CefColor DARK_MODE_COLOR = new CefColor(255, 32, 33, 35);

	public static boolean DARK_MODE = false;

	public static void setDarkMode(boolean value) {
		DARK_MODE = value;
	}

	private long color_value = 0;

	public CefColor(int alpha, int red, int green, int blue) {
		color_value = (alpha << 24) | (red << 16) | (green << 8) | (blue << 0);
	}

	public long getColor() {
		return color_value;
	}

	public int getAlpha() {
		return (int) ((color_value >> 24) & 0xFF);
	}

	public int getRed() {
		return (int) ((color_value >> 16) & 0xFF);
	}

	public int getGreen() {
		return (int) ((color_value >> 8) & 0xFF);
	}

	public int getBlue() {
		return (int) (color_value & 0xFF);
	}
}
