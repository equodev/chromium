package org.eclipse.swt.internal.chromium.lib;

import org.eclipse.swt.internal.Callback;

///
/// Structure used to handle file downloads. The functions of this structure will
/// called on the browser process UI thread.
///
public class cef_download_handler_t {
	///
	/// Base structure.
	///
	public cef_base_ref_counted_t base;
	///
	/// Called before a download begins. |suggested_name| is the suggested name for
	/// the download file. By default the download will be canceled. Execute
	/// |callback| either asynchronously or in this function to continue the
	/// download if desired. Do not keep a reference to |download_item| outside of
	/// this function.
	///
	/** @field cast=(void*) */
	public long /*int*/ on_before_download;
	///
	/// Called when a download's status or progress information has been updated.
	/// This may be called multiple times before and after on_before_download().
	/// Execute |callback| either asynchronously or in this function to cancel the
	/// download if desired. Do not keep a reference to |download_item| outside of
	/// this function.
	///
	/** @field cast=(void*) */
	public long /*int*/ on_download_updated;
	
	/** @field flags=no_gen */
	public long /*int*/ ptr;
	/** @field flags=no_gen */
	public Callback on_before_download_cb;
	/** @field flags=no_gen */
	public Callback on_download_updated_cb;
	
	public static final int sizeof = ChromiumLib.cef_download_handler_t_sizeof();

}
