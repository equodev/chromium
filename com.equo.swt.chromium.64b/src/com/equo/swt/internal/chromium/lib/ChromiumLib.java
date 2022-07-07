/****************************************************************************
**
** Copyright (C) 2021 Equo
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

package com.equo.swt.internal.chromium.lib;

import org.eclipse.swt.internal.C;

import com.equo.swt.internal.chromium.lib.FunctionSt;
import com.equo.swt.internal.chromium.lib.cef_app_t;
import com.equo.swt.internal.chromium.lib.cef_browser_process_handler_t;
import com.equo.swt.internal.chromium.lib.cef_client_t;
import com.equo.swt.internal.chromium.lib.cef_context_menu_handler_t;
import com.equo.swt.internal.chromium.lib.cef_cookie_visitor_t;
import com.equo.swt.internal.chromium.lib.cef_display_handler_t;
import com.equo.swt.internal.chromium.lib.cef_focus_handler_t;
import com.equo.swt.internal.chromium.lib.cef_jsdialog_handler_t;
import com.equo.swt.internal.chromium.lib.cef_life_span_handler_t;
import com.equo.swt.internal.chromium.lib.cef_load_handler_t;
import com.equo.swt.internal.chromium.lib.cef_popup_features_t;
import com.equo.swt.internal.chromium.lib.cef_request_handler_t;
import com.equo.swt.internal.chromium.lib.cef_string_visitor_t;

public class ChromiumLib extends C {

  /**
   * @param app cast=(void *)
   */
  public static final native void cefswt_init(long /* int */ app, String cefrustPath,
      String version, int debugPort);

  /**
   * @param hwnd          cast=(void *)
   * @param clientHandler cast=(void *)
   */
  public static final native long /* int */ cefswt_create_browser(long /* int */ hwnd, String url,
      long /* int */ clientHandler, int w, int h, int js, int cefBgColor);

  public static final native int cefswt_do_message_loop_work();

  /**
   * @param browser cast=(void *)
   */
  public static final native void cefswt_load_url(long /* int */ browser, String url, byte[] bytes,
      int length, String headers, int length2);

  /**
   * @param browser cast=(void *)
   */
  public static final native int cefswt_get_id(long /* int */ browser);

  /**
   * @param browser cast=(void *)
   */
  public static final native long /* int */ cefswt_get_url(long /* int */ browser);

  /**
   * @param browser cast=(void *)
   * @param visitor cast=(void *)
   */
  public static final native void cefswt_get_text(long /* int */ browser, long /* int */ visitor);

  /**
   * @param browser cast=(void *)
   */
  public static final native void cefswt_reload(long /* int */ browser);

  /**
   * @param browser cast=(void *)
   */
  public static final native void cefswt_go_forward(long /* int */ browser);

  /**
   * @param browser cast=(void *)
   */
  public static final native void cefswt_go_back(long /* int */ browser);

  /**
   * @param windowInfo    cast=(void *)
   * @param client        cast=(void *)
   * @param clientHandler cast=(void *)
   * @param handle        cast=(void *)
   */
  public static final native void cefswt_set_window_info_parent(long /* int */ windowInfo,
      long /* int */ client, long /* int */ clientHandler, long /* int */ handle, int x, int y,
      int w, int h);

  /**
   * @param browser cast=(void *)
   */
  public static final native void cefswt_resized(long /* int */ browser, int width, int height);

  /**
   * @param browser    cast=(void *)
   * @param shell_hwnd cast=(void *)
   */
  public static final native void cefswt_set_focus(long /* int */ browser, boolean focus,
      long /* int */ shell_hwnd);

  /**
   * @param browser cast=(void *)
   * @param that    cast=(void *)
   */
  public static final native boolean cefswt_is_same(long /* int */ browser, long /* int */ that);

  /**
   * @param frame cast=(void *)
   */
  public static final native boolean cefswt_is_main_frame(long /* int */ frame);

  /**
   * @param bs cast=(void *)
   */
  public static final native void cefswt_free(long /* int */ bs);

  /**
   * @param browser cast=(void *)
   */
  public static final native void cefswt_stop(long /* int */ browser);

  /**
   * @param browser cast=(void *)
   */
  public static final native void cefswt_close_browser(long /* int */ browser);

  /**
   * @param browser cast=(void *)
   */
  public static final native void cefswt_execute(long /* int */ browser, String script);

  /**
   * @param browser  cast=(void *)
   * @param callback cast=(void *)
   */
  public static final native boolean cefswt_eval(long /* int */ browser, String script, int id,
      long /* int */ callback);

  /**
   * @param msg      cast=(void *)
   * @param callback cast=(void *)
   */
  public static final native boolean cefswt_function_arg(long /* int */ msg, int index,
      long /* int */ callback);

  /**
   * @param browser cast=(void *)
   */
  public static final native boolean cefswt_function(long /* int */ browser, String name, int id);

  /**
   * @param msg cast=(void *)
   * @param ret flags=no_in
   */
  public static final native void cefswt_function_id(long /* int */ msg, FunctionSt ret);

  /**
   * @param browser cast=(void *)
   */
  public static final native boolean cefswt_function_return(long /* int */ browser, int id,
      int port, int returnType, String ret);

  /** @method flags=no_gen */
  public static final native String cefswt_cstring_to_java(long /* int */ string);

  /** @method flags=no_gen */
  public static final native String cefswt_cefstring_to_java(long /* int */ string);

  /** @method flags=no_gen */
  public static final native String cefswt_request_to_java(long /* int */ request);

  /** @method flags=no_gen */
  public static final native String cefswt_cookie_to_java(long /* int */ cookie);

  /**
   * @param callback            cast=(void *)
   * @param default_prompt_text cast=(void *)
   */
  public static final native void cefswt_dialog_close(long /* int */ callback, int i,
      long /* int */ default_prompt_text);

  /**
   * @param callback cast=(void *)
   */
  public static final native void cefswt_context_menu_cancel(long /* int */ callback);

  /**
   * @param callback cast=(void *)
   */
  public static final native void cefswt_auth_callback(long /* int */ callback, String user,
      String password, int cont);

  /**
   * @param browser cast=(void *)
   * @param event   cast=(void *)
   */
  public static final native int cefswt_handlekey(long /* int */ browser, long /* int */ event);

  public static final native void cefswt_shutdown();

  public static final native boolean cefswt_set_cookie(String url, String name, String value,
      String domain, String path, int secure, int httpOnly, double maxAge);

  /**
   * @param visitor cast=(void *)
   */
  public static final native boolean cefswt_get_cookie(String url, long /* int */ visitor);

  public static final native void cefswt_delete_cookies();

  /** @method flags=no_gen */
  public static final native String cefswt_cookie_value(long /* int */ cookie);

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

  public static final native int cef_focus_handler_t_sizeof();

  public static final native int cef_popup_features_t_sizeof();

  public static final native int cef_key_event_t_sizeof();

  public static final native int cef_string_visitor_t_sizeof();

  public static final native int cef_cookie_visitor_t_sizeof();

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_app_t src, int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_browser_process_handler_t src,
      int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_client_t src, int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_life_span_handler_t src,
      int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_load_handler_t src, int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_display_handler_t src, int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_request_handler_t src, int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_jsdialog_handler_t src,
      int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_context_menu_handler_t src,
      int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_focus_handler_t src, int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_keyboard_handler_t src,
      int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_string_visitor_t src, int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(long /* int */ dest, cef_cookie_visitor_t src, int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(cef_popup_features_t dest, long /* int */ src, int size);

  /**
   * @param dest cast=(void *)
   * @param src  cast=(const void *),flags=no_out
   * @param size cast=(size_t)
   */
  public static final native void memmove(cef_key_event_t dest, long /* int */ src, int size);

}
