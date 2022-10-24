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

public class Point {
	/**
	 * the x coordinate of the point
	 */
	public int x;

	/**
	 * the y coordinate of the point
	 */
	public int y;

	/**
	 * Constructs a new point with the given x and y coordinates.
	 *
	 * @param x the x coordinate of the new point
	 * @param y the y coordinate of the new point
	 */
	public Point (int x, int y) {
		this.x = x;
		this.y = y;
	}

    /**
     * Translates this point, at location {@code (x,y)},
     * by {@code dx} along the {@code x} axis and {@code dy}
     * along the {@code y} axis so that it now represents the point
     * {@code (x+dx,y+dy)}.
     *
     * @param       dx   the distance to move this point
     *                            along the X axis
     * @param       dy    the distance to move this point
     *                            along the Y axis
     */
    public void translate(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    /**
     * Constructs and initializes a point with the same location as
     * the specified <code>Point</code> object.
     * @param       p a point
     */
    public Point(Point p) {
        this(p.x, p.y);
    }
}
