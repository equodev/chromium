package org.eclipse.swt.internal.chromium.lib;

public class DownloadItem {
    public int id;
    public int status;
    /** @field cast=(void*) */
    public long /*int*/ file;
    /** @field cast=(void*) */
    public long /*int*/ url;
    public long received;
    public long total;
    public long speed;
    public int percent;
}