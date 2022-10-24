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


package com.equo.chromium.swt.internal;

import org.cef.callback.CefDownloadItem;

/**
 * This class used for obtained CefDwonloadItem values and used into asyncExec
 * method.
 */
public class DownloadItem {
	public final int id;
	public final boolean isValid;
	public final boolean isComplete;
	public final boolean isCanceled;
	public final String url;
	public final String fullPath;
	public final long receivedBytes;
	public final long totalBytes;
	public final int percentComplete;
	public final long currentSpeed;

	public DownloadItem(CefDownloadItem cefDownloadItem) {
		this.id = cefDownloadItem.getId();
		this.isValid = cefDownloadItem.isValid();
		this.isComplete = cefDownloadItem.isComplete();
		this.isCanceled = cefDownloadItem.isCanceled();
		this.url = cefDownloadItem.getURL();
		this.fullPath = cefDownloadItem.getFullPath();
		this.receivedBytes = cefDownloadItem.getReceivedBytes();
		this.totalBytes = cefDownloadItem.getTotalBytes();
		this.percentComplete = cefDownloadItem.getPercentComplete();
		this.currentSpeed = cefDownloadItem.getCurrentSpeed();
	}
}
