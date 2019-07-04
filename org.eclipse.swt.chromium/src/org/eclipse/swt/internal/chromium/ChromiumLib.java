package org.eclipse.swt.internal.chromium;

import org.eclipse.swt.internal.*;

public class ChromiumLib extends C {

    static {
        Library.loadLibrary("swt-chromium");
    }

    public static final native void cefswt_init(cef_app_t app, String cefrustPath, String version, int debugPort);
    public static final void _cefswt_init(cef_app_t app, String cefrustPath, String version, int debugPort) {
        lock.lock();
        try {
            cefswt_init(app, cefrustPath, version, debugPort);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param clientHandler cast=(void *)
     */
    public static final native void cefswt_create_browser(long hwnd, String url, cef_client_t clientHandler, int w, int h, int js, int cefBgColor);
    public static final void _cefswt_create_browser(long hwnd, String url, cef_client_t clientHandler, int w, int h, int js, int cefBgColor) {
        lock.lock();
        try {
            cefswt_create_browser(hwnd, url, clientHandler, w, h, js, cefBgColor);
        } finally {
            lock.unlock();
        }
    }


    public static final native void cefswt_do_message_loop_work();
    public static final void _cefswt_do_message_loop_work() {
        lock.lock();
        try {
        	cefswt_do_message_loop_work();
        } finally {
            lock.unlock();
        }
    }
//    /**
//     * @param browser cast=(void *)
//     * @param headers cast=(const char *const *)
//     */
//    public static final native void cefswt_load_url(long browser, String url, byte[] bytes, int length, String headers, int length2);
//
//    /**
//     * @param browser cast=(void *)
//     */
//    public static final native String cefswt_get_url(long browser);
//
//    /**
//     * @param browser cast=(void *)
//     */
//    public static final native void cefswt_get_text(long browser, cef_string_visitor_t visitor);
//
//    /**
//     * @param browser cast=(void *)
//     */
//    public static final native void cefswt_reload(long browser);
//
//    /**
//     * @param browser cast=(void *)
//     */
//    public static final native void cefswt_go_forward(long browser);
//
//    /**
//     * @param browser cast=(void *)
//     */
//    public static final native void cefswt_go_back(long browser);
//
//    /**
//     * @param windowInfo cast=(void *)
//     * @param client cast=(void *)
//     * @param clientHandler ,flags=critical
//     */
//    public static final native void cefswt_set_window_info_parent(long windowInfo, long client, cef_client_t clientHandler, long handle, int x, int y, int w, int h);
//
//    /** @param browser cast=(void *) */
//    public static final native void cefswt_resized(long browser, int width, int height);
//
//    /**
//     * @param browser cast=(void *)
//     * @param shell_hwnd cast=(void *)
//     */
//    public static final native void cefswt_set_focus(long browser, boolean focus, long shell_hwnd);
//
//    /**
//     * @param browser cast=(void *),flags=critical
//     * @param that cast=(void *),flags=critical
//     */
//    public static final native boolean cefswt_is_same(long browser, long that);
//
//    /** @param bs cast=(void *),flags=critical */
//    public static final native void cefswt_free(long bs);
//
//    /**
//     * @param browser cast=(void *)
//     */
//    public static final native void cefswt_stop(long browser);
//
//    /**
//     * @param browser cast=(void *)
//     */
//    public static final native void cefswt_close_browser(long browser);
//
//    /**
//     * @param browser cast=(void *)
//     */
//    public static final native void cefswt_execute(long browser, String script);
//
//    /**
//     * @param browser cast=(void *)
//     * @param callback cast=(void *)
//     */
//    public static final native boolean cefswt_eval(long browser, String script, int id, long callback);
//
//    /**
//     * @param browser cast=(void *)
//     */
//    public static final native boolean cefswt_function(long browser, String name, int id);
//
//    /**
//     * @param browser cast=(void *)
//     */
//    public static final native boolean cefswt_function_return(long browser, int id, int port, int returnType, String ret);
//
//    /**
//     * @param string cast=(void *)
//     */
//    public static final native String cefswt_cefstring_to_java(long string);
//
//    public static final native void cefswt_shutdown();
//
    public static final native int cef_app_t_sizeof();
//
    public static final native int cef_browser_process_handler_t_sizeof();
//
    public static final native int cef_client_t_sizeof();
//
    public static final native int cef_life_span_handler_t_sizeof();
//
//    public static final native int cef_popup_features_t_sizeof();
//
//    public static final native int cef_string_visitor_t_sizeof();
//
    /**
     * @param dest cast=(void *)
     * @param src cast=(const void *),flags=no_out
     * @param size cast=(size_t)
     */
    public static final native void memmove (long dest, cef_browser_process_handler_t src, long size);
//
//    /**
//     * @param dest cast=(void *)
//     * @param src cast=(const void *),flags=no_out
//     * @param size cast=(size_t)
//     */
//    public static final native void memmove (long dest, cef_client_t src, long size);
//
    /**
     * @param dest cast=(void *)
     * @param src cast=(const void *),flags=no_out
     * @param size cast=(size_t)
     */
    public static final native void memmove (long dest, cef_life_span_handler_t src, long size);
//
//    /**
//     * @param src cast=(const void *)
//     * @param size cast=(size_t)
//     */
//    public static final native void memmove(cef_popup_features_t dest, long src, int size);

}
