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
