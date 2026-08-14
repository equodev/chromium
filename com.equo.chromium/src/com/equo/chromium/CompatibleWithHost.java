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
package com.equo.chromium;

import java.util.concurrent.CompletableFuture;

public interface CompatibleWithHost {

	/**
	 * Configure the compatibility check for the windowless toolkit.
	 */
	public CompatibleWithHost windowless();

	/**
	 * Performs the toolkit check. Returns a completableFuture<String> completed
	 * with empty if the test was successful or exceptionally completed with error.
	 * The test details for both cases are generated in the
	 * ~/.equo/compatibility_<DATE>.log file.
	 */
	public CompletableFuture<String> check();
}
