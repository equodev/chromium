/****************************************************************************
**
** Copyright (C) 2022 Equo
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


package org.cef.misc;

public class Rectangle {
	/**
	 * the x coordinate of the rectangle
	 */
	public int x;

	/**
	 * the y coordinate of the rectangle
	 */
	public int y;

	/**
	 * the width of the rectangle
	 */
	public int width;

	/**
	 * the height of the rectangle
	 */
	public int height;
	/**
	 * Construct a new instance of this class given the
	 * x, y, width and height values.
	 *
	 * @param x the x coordinate of the origin of the rectangle
	 * @param y the y coordinate of the origin of the rectangle
	 * @param width the width of the rectangle
	 * @param height the height of the rectangle
	 */
	public Rectangle (int x, int y, int width, int height) {
		setBounds(x, y, width, height);
	}

	public void setBounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public Rectangle getBounds(){
		return new Rectangle(x, y, width, height);
	}

	public Rectangle(Point point, Object dimension) {
		int w = 0;
		int h = 0;
		try {
			w = (int) dimension.getClass().getField("width").get(dimension);
			h = (int) dimension.getClass().getField("height").get(dimension);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			
		}
		setBounds(point.x, point.y, w, h);
	}

	public Rectangle (Object rect) {
		setBounds(rect);
	}

	public Rectangle clone() {
		return new Rectangle(x, y, width, height);
	}

	private void setBounds(Object rect) {
		try {
			setBounds((int) rect.getClass().getField("x").get(rect), (int) rect.getClass().getField("y").get(rect),
					(int) rect.getClass().getField("width").get(rect),
					(int) rect.getClass().getField("height").get(rect));
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
		}
	}
}
