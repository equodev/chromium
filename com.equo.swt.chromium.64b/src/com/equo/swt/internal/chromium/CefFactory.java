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

package com.equo.swt.internal.chromium;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.internal.Library;

import com.equo.swt.internal.chromium.lib.cef_app_t;
import com.equo.swt.internal.chromium.lib.cef_base_ref_counted_t;
import com.equo.swt.internal.chromium.lib.cef_browser_process_handler_t;
import com.equo.swt.internal.chromium.lib.cef_client_t;
import com.equo.swt.internal.chromium.lib.cef_context_menu_handler_t;
import com.equo.swt.internal.chromium.lib.cef_cookie_visitor_t;
import com.equo.swt.internal.chromium.lib.cef_display_handler_t;
import com.equo.swt.internal.chromium.lib.cef_focus_handler_t;
import com.equo.swt.internal.chromium.lib.cef_jsdialog_handler_t;
import com.equo.swt.internal.chromium.lib.cef_keyboard_handler_t;
import com.equo.swt.internal.chromium.lib.cef_life_span_handler_t;
import com.equo.swt.internal.chromium.lib.cef_load_handler_t;
import com.equo.swt.internal.chromium.lib.cef_request_handler_t;
import com.equo.swt.internal.chromium.lib.cef_string_visitor_t;

public class CefFactory {

  public static final int PID_BROWSER = 0;
  public static final int PID_RENDERER = 1;

  public static void create(String cefrustPath) {
    boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
    boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
    List<String> toLoad = new ArrayList<>();
    //        String cefrustPath = System.getProperty("cefswt.path", "");
    if (isWin) {
      toLoad.add(cefrustPath + File.separator + "chrome_elf.dll");
      toLoad.add(cefrustPath + File.separator + "libcef.dll");
    } else if (isMac) {
      toLoad
          .add(cefrustPath + "/Chromium Embedded Framework.framework/Chromium Embedded Framework");
    } else {
      toLoad.add(cefrustPath + "/libcef.so");
    }

    for (String lib : toLoad) {
      try {
        Library.loadLibrary(lib, false);
      } catch (UnsatisfiedLinkError e) {
        if (!isWin && !isMac && e.getMessage().contains("libgconf")) {
          try {
            Library.loadLibrary(cefrustPath + "/libgconf-2.so.4", false);
          } catch (UnsatisfiedLinkError e1) {
            throw e;
          }
          Library.loadLibrary(lib, false);
          return;
        }
        throw e;
      }
    }
  }

  public static cef_app_t newApp() {
    cef_app_t st = new cef_app_t();
    st.base = setBase(st, cef_app_t.sizeof);
    return st;
  }

  public static cef_browser_process_handler_t newBrowserProcessHandler() {
    cef_browser_process_handler_t st = new cef_browser_process_handler_t();
    st.base = setBase(st, cef_browser_process_handler_t.sizeof);
    return st;
  }

  public static cef_client_t newClient() {
    cef_client_t st = new cef_client_t();
    st.base = setBase(st, cef_client_t.sizeof);
    return st;
  }

  public static cef_focus_handler_t newFocusHandler() {
    cef_focus_handler_t st = new cef_focus_handler_t();
    st.base = setBase(st, cef_focus_handler_t.sizeof);
    return st;
  }

  public static cef_keyboard_handler_t newKeyboardHandler() {
    cef_keyboard_handler_t st = new cef_keyboard_handler_t();
    st.base = setBase(st, cef_keyboard_handler_t.sizeof);
    return st;
  }

  public static cef_life_span_handler_t newLifeSpanHandler() {
    cef_life_span_handler_t st = new cef_life_span_handler_t();
    st.base = setBase(st, cef_life_span_handler_t.sizeof);
    return st;
  }

  public static cef_load_handler_t newLoadHandler() {
    cef_load_handler_t st = new cef_load_handler_t();
    st.base = setBase(st, cef_load_handler_t.sizeof);
    return st;
  }

  public static cef_display_handler_t newDisplayHandler() {
    cef_display_handler_t st = new cef_display_handler_t();
    st.base = setBase(st, cef_display_handler_t.sizeof);
    return st;
  }

  public static cef_request_handler_t newRequestHandler() {
    cef_request_handler_t st = new cef_request_handler_t();
    st.base = setBase(st, cef_request_handler_t.sizeof);
    return st;
  }

  public static cef_jsdialog_handler_t newJsDialogHandler() {
    cef_jsdialog_handler_t st = new cef_jsdialog_handler_t();
    st.base = setBase(st, cef_jsdialog_handler_t.sizeof);
    return st;
  }

  public static cef_context_menu_handler_t newContextMenuHandler() {
    cef_context_menu_handler_t st = new cef_context_menu_handler_t();
    st.base = setBase(st, cef_context_menu_handler_t.sizeof);
    return st;
  }

  public static cef_string_visitor_t newStringVisitor() {
    cef_string_visitor_t st = new cef_string_visitor_t();
    st.base = setBase(st, cef_string_visitor_t.sizeof);
    return st;
  }

  public static cef_cookie_visitor_t newCookieVisitor() {
    cef_cookie_visitor_t st = new cef_cookie_visitor_t();
    st.base = setBase(st, cef_cookie_visitor_t.sizeof);
    return st;
  }

  private static cef_base_ref_counted_t setBase(Object st, int sizeof) {
    //        System.out.println("J:SIZEOF:" + st.getClass().getSimpleName() + ":" + sizeof);
    com.equo.swt.internal.chromium.lib.cef_base_ref_counted_t base =
        new com.equo.swt.internal.chromium.lib.cef_base_ref_counted_t();
    base.size = sizeof;
    base.add_ref = 0;
    base.has_one_ref = 0;
    base.release = 0;
    //        base.name = st.getClass().getSimpleName();
    return base;
  }

  public static enum ReturnType {
    Double(0), Bool(1), Str(2), Null(3), Array(4), Error(5);

    private int value;

    private ReturnType(int value) {
      this.value = value;
    }

    public int intValue() {
      return value;
    }

    public static ReturnType from(String v) {
      try {
        int value = Integer.parseInt(v);
        for (ReturnType rt : ReturnType.values()) {
          if (rt.intValue() == value) {
            return rt;
          }
        }
      } catch (NumberFormatException e) {
        // Undefined behavior
      }
      throw new IllegalArgumentException(v);
    }
  }

}
