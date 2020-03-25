package org.eclipse.swt.internal.chromium.lib;

import org.eclipse.swt.internal.Callback;

///
/// Implement this structure to handle events related to browser requests. The
/// functions of this structure will be called on the thread indicated.
///
public class cef_request_handler_t {
	///
	/// Base structure.
	///
	public cef_base_ref_counted_t base;
	///
	/// Called on the UI thread before browser navigation. Return true (1) to
	/// cancel the navigation or false (0) to allow the navigation to proceed. The
	/// |request| object cannot be modified in this callback.
	/// cef_load_handler_t::OnLoadingStateChange will be called twice in all cases.
	/// If the navigation is allowed cef_load_handler_t::OnLoadStart and
	/// cef_load_handler_t::OnLoadEnd will be called. If the navigation is canceled
	/// cef_load_handler_t::OnLoadError will be called with an |errorCode| value of
	/// ERR_ABORTED.
	///
	/** @field cast=(void*) */
	public long /*int*/ on_before_browse;
	///
	/// Called on the UI thread before OnBeforeBrowse in certain limited cases
	/// where navigating a new or different browser might be desirable. This
	/// includes user-initiated navigation that might open in a special way (e.g.
	/// links clicked via middle-click or ctrl + left-click) and certain types of
	/// cross-origin navigation initiated from the renderer process (e.g.
	/// navigating the top-level frame to/from a file URL). The |browser| and
	/// |frame| values represent the source of the navigation. The
	/// |target_disposition| value indicates where the user intended to navigate
	/// the browser based on standard Chromium behaviors (e.g. current tab, new
	/// tab, etc). The |user_gesture| value will be true (1) if the browser
	/// navigated via explicit user gesture (e.g. clicking a link) or false (0) if
	/// it navigated automatically (e.g. via the DomContentLoaded event). Return
	/// true (1) to cancel the navigation or false (0) to allow the navigation to
	/// proceed in the source browser's top-level frame.
	///
	/** @field cast=(void*) */
	public long /*int*/ on_open_urlfrom_tab;
	/// Called on the browser process IO thread before a resource request is
	/// initiated. The |browser| and |frame| values represent the source of the
	/// request. |request| represents the request contents and cannot be modified
	/// in this callback. |is_navigation| will be true (1) if the resource request
	/// is a navigation. |is_download| will be true (1) if the resource request is
	/// a download. |request_initiator| is the origin (scheme + domain) of the page
	/// that initiated the request. Set |disable_default_handling| to true (1) to
	/// disable default handling of the request, in which case it will need to be
	/// handled via cef_resource_request_handler_t::GetResourceHandler or it will
	/// be canceled. To allow the resource load to proceed with default handling
	/// return NULL. To specify a handler for the resource return a
	/// cef_resource_request_handler_t object. If this callback returns NULL the
	/// same function will be called on the associated cef_request_tContextHandler,
	/// if any.
	///
	/** @field cast=(void*) */
	public long /*int*/ get_resource_request_handler;
	///
	/// Called on the IO thread when the browser needs credentials from the user.
	/// |isProxy| indicates whether the host is a proxy server. |host| contains the
	/// hostname and |port| contains the port number. |realm| is the realm of the
	/// challenge and may be NULL. |scheme| is the authentication scheme used, such
	/// as "basic" or "digest", and will be NULL if the source of the request is an
	/// FTP server. Return true (1) to continue the request and call
	/// cef_auth_callback_t::cont() either in this function or at a later time when
	/// the authentication information is available. Return false (0) to cancel the
	/// request immediately.
	///
	/** @field cast=(void*) */
	public long /*int*/ get_auth_credentials;
	///
	/// Called on the IO thread when JavaScript requests a specific storage quota
	/// size via the webkitStorageInfo.requestQuota function. |origin_url| is the
	/// origin of the page making the request. |new_size| is the requested quota
	/// size in bytes. Return true (1) to continue the request and call
	/// cef_request_tCallback::cont() either in this function or at a later time to
	/// grant or deny the request. Return false (0) to cancel the request
	/// immediately.
	///
	/** @field cast=(void*) */
	public long /*int*/ on_quota_request;
	///
	/// Called on the UI thread to handle requests for URLs with an invalid SSL
	/// certificate. Return true (1) and call cef_request_tCallback::cont() either
	/// in this function or at a later time to continue or cancel the request.
	/// Return false (0) to cancel the request immediately. If
	/// CefSettings.ignore_certificate_errors is set all invalid certificates will
	/// be accepted without calling this function.
	///
	/** @field cast=(void*) */
	public long /*int*/ on_certificate_error;
	///
	/// Called on the UI thread when a client certificate is being requested for
	/// authentication. Return false (0) to use the default behavior and
	/// automatically select the first certificate available. Return true (1) and
	/// call cef_select_client_certificate_callback_t::Select either in this
	/// function or at a later time to select a certificate. Do not call Select or
	/// call it with NULL to continue without using any certificate. |isProxy|
	/// indicates whether the host is an HTTPS proxy or the origin server. |host|
	/// and |port| contains the hostname and port of the SSL server. |certificates|
	/// is the list of certificates to choose from; this list has already been
	/// pruned by Chromium so that it only contains certificates from issuers that
	/// the server trusts.
	///
	/** @field cast=(void*) */
	public long /*int*/ on_select_client_certificate;
	///
	/// Called on the browser process UI thread when a plugin has crashed.
	/// |plugin_path| is the path of the plugin that crashed.
	///
	/** @field cast=(void*) */
	public long /*int*/ on_plugin_crashed;
	///
	/// Called on the browser process UI thread when the render view associated
	/// with |browser| is ready to receive/handle IPC messages in the render
	/// process.
	///
	/** @field cast=(void*) */
	public long /*int*/ on_render_view_ready;
	///
	/// Called on the browser process UI thread when the render process terminates
	/// unexpectedly. |status| indicates how the process terminated.
	///
	/** @field cast=(void*) */
	public long /*int*/ on_render_process_terminated;

	/** @field flags=no_gen */
	public long /*int*/ ptr;
	/** @field flags=no_gen */
	public Callback on_before_browse_cb;
	/** @field flags=no_gen */
	public Callback get_auth_credentials_cb;

	public static final int sizeof = ChromiumLib.cef_request_handler_t_sizeof();
}