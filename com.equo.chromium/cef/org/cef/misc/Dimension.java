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

public class Dimension {

    /**
     * The width dimension; negative values can be used.
     */
    public int width;

    /**
     * The height dimension; negative values can be used.
     */
    public int height;

    /**
     * Creates an instance of <code>Dimension</code> with a width
     * of zero and a height of zero.
     */
    public Dimension() {
        this(0, 0);
    }

    /**
     * Constructs a <code>Dimension</code> and initializes
     * it to the specified width and specified height.
     *
     * @param width the specified width
     * @param height the specified height
     */
    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

}
