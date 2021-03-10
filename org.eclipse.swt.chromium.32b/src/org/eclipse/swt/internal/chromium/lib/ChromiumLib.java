package org.eclipse.swt.internal.chromium.lib;

import org.eclipse.swt.internal.*;

public class ChromiumLib extends C {

	/**
	 * @param app cast=(void *)
	 */
	public static final native void cefswt_init(int /*long*/ app, String cefrustPath, String version, int debugPort);

	/**
	 * @param hwnd cast=(void *)
	 * @param clientHandler cast=(void *)
	 */
	public static final native int /*long*/ cefswt_create_browser(int /*long*/ hwnd, String url, int /*long*/ clientHandler, int w, int h, int js, int cefBgColor);

	/**
	 * @param cmd cast=(void *)
	 */
	public static final native void cefswt_set_command(int /*long*/ cmd, String arg, String val);

	public static final native int cefswt_do_message_loop_work();

	/**
	 * @param browser cast=(void *)
	 */
	public static final native void cefswt_load_url(int /*long*/ browser, String url, byte[] bytes, int length, String headers, int length2);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native int cefswt_get_id(int /*long*/ browser);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native int /*long*/ cefswt_get_url(int /*long*/ browser);

	/**
	 * @param browser cast=(void *)
	 * @param visitor cast=(void *)
	 */
	public static final native void cefswt_get_text(int /*long*/ browser, int /*long*/ visitor);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native void cefswt_reload(int /*long*/ browser);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native void cefswt_go_forward(int /*long*/ browser);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native void cefswt_go_back(int /*long*/ browser);

	/**
	 * @param windowInfo cast=(void *)
	 * @param client cast=(void *)
	 * @param clientHandler cast=(void *)
	 * @param handle cast=(void *)
	 */
	public static final native void cefswt_set_window_info_parent(int /*long*/ windowInfo, int /*long*/ client, int /*long*/ clientHandler, int /*long*/ handle, int x, int y, int w, int h);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native void cefswt_resized(int /*long*/ browser, int width, int height);

	/**
	 * @param browser cast=(void *)
	 * @param shell_hwnd cast=(void *)
	 */
	public static final native void cefswt_set_focus(int /*long*/ browser, boolean focus, int /*long*/ shell_hwnd);

	/**
	 * @param browser cast=(void *)
	 * @param that cast=(void *)
	 */
	public static final native boolean cefswt_is_same(int /*long*/ browser, int /*long*/ that);

	/**
	 * @param frame cast=(void *)
	 */
	public static final native boolean cefswt_is_main_frame(int /*long*/ frame);

	/**
	 * @param bs cast=(void *)
	 */
	public static final native void cefswt_free(int /*long*/ bs);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native int cefswt_ref(int /*long*/ browser, int add);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native void cefswt_stop(int /*long*/ browser);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native void cefswt_close_browser(int /*long*/ browser, int force);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native void cefswt_execute(int /*long*/ browser, String script);

	/**
	 * @param browser cast=(void *)
	 * @param callback cast=(void *)
	 */
	public static final native boolean cefswt_eval(int /*long*/ browser, String script, int id, int /*long*/ callback);

	/**
	 * @param msg cast=(void *)
	 * @param callback cast=(void *)
	 */
	public static final native boolean cefswt_function_arg(int /*long*/ msg, int index, int /*long*/ callback);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native boolean cefswt_function(int /*long*/ browser, String name, int id);

	/**
	 * @param msg cast=(void *)
	 * @param ret flags=no_in
	 */
	public static final native void cefswt_function_id(int /*long*/ msg, FunctionSt ret);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native boolean cefswt_function_return(int /*long*/ browser, int id, int port, int returnType, String ret);

	/** @method flags=no_gen */
	public static final native String cefswt_cstring_to_java(int /*long*/ string, boolean free);

	/** @method flags=no_gen */
	public static final native String cefswt_cefstring_to_java(int /*long*/ string);

	/** @method flags=no_gen */
	public static final native String cefswt_request_to_java(int /*long*/ request);

	/** @method flags=no_gen */
	public static final native String cefswt_cookie_to_java(int /*long*/ cookie);

	/**
	 * @param callback cast=(void *)
	 * @param default_prompt_text cast=(void *)
	 */
	public static final native void cefswt_dialog_close(int /*long*/ callback, int i, int /*long*/ default_prompt_text);

	/**
	 * @param callback cast=(void *)
	 */
	public static final native void cefswt_context_menu_cancel(int /*long*/ callback);

	/**
	 * @param callback cast=(void *)
	 */
	public static final native void cefswt_auth_callback(int /*long*/ callback, String user, String password, int cont);

	/**
	 * @param callback cast=(void *)
	 * @param ssl_info cast=(void *)
	 */
	public static final native void cefswt_request_callback(int /* long */ callback, int /* long */ ssl_info, byte[] bytes, int length, int cont);

	/**
	 * @param callback cast=(void *)
	 */
	public static final native void cefswt_download_callback(int /*long*/ callback, String selected, int cont);
	/**
	 * @param download_item cast=(void *)
	 * @param item flags=no_in
	 */
	public static final native void cefswt_download_item(int /*long*/ download_item, DownloadItem item);

	/**
	 * @param browser cast=(void *)
	 * @param event cast=(void *)
	 */
	public static final native int cefswt_handlekey(int /*long*/ browser, int /*long*/ event);

	public static final native void cefswt_shutdown();

	public static final native boolean cefswt_set_cookie(String url, String name, String value, String domain, String path, int secure, int httpOnly, double maxAge);

	/**
	 * @param visitor cast=(void *)
	 */
	public static final native boolean cefswt_get_cookie(String url, int /*long*/ visitor);

	public static final native void cefswt_delete_cookies();

	/** @method flags=no_gen */
	public static final native String cefswt_cookie_value(int /*long*/ cookie);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native void cefswt_find(int /*long*/ browser, String search, int forward, int matchCase, int findNext);

	/**
	 * @param browser cast=(void *)
	 */
	public static final native void cefswt_zoom(int /*long*/ browser, double level);

	public static final native int cef_app_t_sizeof();

	public static final native int cef_browser_process_handler_t_sizeof();

	public static final native int cef_client_t_sizeof();

	public static final native int cef_life_span_handler_t_sizeof();

	public static final native int cef_load_handler_t_sizeof();

	public static final native int cef_keyboard_handler_t_sizeof();

	public static final native int cef_display_handler_t_sizeof();

	public static final native int cef_request_handler_t_sizeof();

	public static final native int cef_jsdialog_handler_t_sizeof();

	public static final native int cef_context_menu_handler_t_sizeof();
	
	public static final native int cef_download_handler_t_sizeof();

	public static final native int cef_focus_handler_t_sizeof();

	public static final native int cef_popup_features_t_sizeof();

	public static final native int cef_key_event_t_sizeof();

	public static final native int cef_string_visitor_t_sizeof();

	public static final native int cef_cookie_visitor_t_sizeof();

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_app_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_browser_process_handler_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_client_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_life_span_handler_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_load_handler_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_display_handler_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_request_handler_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_jsdialog_handler_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_context_menu_handler_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_download_handler_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_focus_handler_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_keyboard_handler_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_string_visitor_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (int /*long*/ dest, cef_cookie_visitor_t src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (cef_popup_features_t dest, int /*long*/ src, int size);

	/**
	 * @param dest cast=(void *)
	 * @param src cast=(const void *),flags=no_out
	 * @param size cast=(size_t)
	 */
	public static final native void memmove (cef_key_event_t dest, int /*long*/ src, int size);

}
