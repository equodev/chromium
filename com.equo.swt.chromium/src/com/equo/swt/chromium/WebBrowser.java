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

package com.equo.swt.chromium;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.AuthenticationListener;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.browser.VisibilityWindowListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

abstract class WebBrowser {
  Browser browser;
  Map<Integer, BrowserFunction> functions = new HashMap<>();
  AuthenticationListener[] authenticationListeners = new AuthenticationListener[0];
  CloseWindowListener[] closeWindowListeners = new CloseWindowListener[0];
  LocationListener[] locationListeners = new LocationListener[0];
  OpenWindowListener[] openWindowListeners = new OpenWindowListener[0];
  ProgressListener[] progressListeners = new ProgressListener[0];
  StatusTextListener[] statusTextListeners = new StatusTextListener[0];
  TitleListener[] titleListeners = new TitleListener[0];
  VisibilityWindowListener[] visibilityWindowListeners = new VisibilityWindowListener[0];
  boolean jsEnabledOnNextPage = true;
  boolean jsEnabled = true;
  int nextFunctionIndex = 1;
  Object evaluateResult;

  static final String ERROR_ID = "org.eclipse.swt.browser.error"; // $NON-NLS-1$
  static final String EXECUTE_ID = "SWTExecuteTemporaryFunction"; // $NON-NLS-1$

  static List<String[]> NativePendingCookies = new ArrayList<>();
  static String CookieName;
  static String CookieValue;
  static String CookieUrl;
  static boolean CookieResult;
  static Runnable NativeClearSessions;
  static Runnable NativeGetCookie;
  static Runnable NativeSetCookie;

  /* Key Mappings */
  static final int[][] KeyTable = {
      /* Keyboard and Mouse Masks */
      { 18, SWT.ALT }, { 16, SWT.SHIFT }, { 17, SWT.CONTROL }, { 224, SWT.COMMAND },

      /* Literal Keys */
      { 65, 'a' }, { 66, 'b' }, { 67, 'c' }, { 68, 'd' }, { 69, 'e' }, { 70, 'f' }, { 71, 'g' },
      { 72, 'h' }, { 73, 'i' }, { 74, 'j' }, { 75, 'k' }, { 76, 'l' }, { 77, 'm' }, { 78, 'n' },
      { 79, 'o' }, { 80, 'p' }, { 81, 'q' }, { 82, 'r' }, { 83, 's' }, { 84, 't' }, { 85, 'u' },
      { 86, 'v' }, { 87, 'w' }, { 88, 'x' }, { 89, 'y' }, { 90, 'z' }, { 48, '0' }, { 49, '1' },
      { 50, '2' }, { 51, '3' }, { 52, '4' }, { 53, '5' }, { 54, '6' }, { 55, '7' }, { 56, '8' },
      { 57, '9' }, { 32, ' ' }, { 59, ';' }, { 61, '=' }, { 188, ',' }, { 190, '.' }, { 191, '/' },
      { 219, '[' }, { 221, ']' }, { 222, '\'' }, { 192, '`' }, { 220, '\\' }, { 108, '|' },
      { 226, '<' },

      /* Non-Numeric Keypad Keys */
      { 37, SWT.ARROW_LEFT }, { 39, SWT.ARROW_RIGHT }, { 38, SWT.ARROW_UP }, { 40, SWT.ARROW_DOWN },
      { 45, SWT.INSERT }, { 36, SWT.HOME }, { 35, SWT.END }, { 46, SWT.DEL }, { 33, SWT.PAGE_UP },
      { 34, SWT.PAGE_DOWN },

      /* Virtual and Ascii Keys */
      { 8, SWT.BS }, { 13, SWT.CR }, { 9, SWT.TAB }, { 27, SWT.ESC }, { 12, SWT.DEL },

      /* Functions Keys */
      { 112, SWT.F1 }, { 113, SWT.F2 }, { 114, SWT.F3 }, { 115, SWT.F4 }, { 116, SWT.F5 },
      { 117, SWT.F6 }, { 118, SWT.F7 }, { 119, SWT.F8 }, { 120, SWT.F9 }, { 121, SWT.F10 },
      { 122, SWT.F11 }, { 123, SWT.F12 }, { 124, SWT.F13 }, { 125, SWT.F14 }, { 126, SWT.F15 },
      { 127, 0 }, { 128, 0 }, { 129, 0 }, { 130, 0 }, { 131, 0 }, { 132, 0 }, { 133, 0 },
      { 134, 0 }, { 135, 0 },

      /* Numeric Keypad Keys */
      { 96, SWT.KEYPAD_0 }, { 97, SWT.KEYPAD_1 }, { 98, SWT.KEYPAD_2 }, { 99, SWT.KEYPAD_3 },
      { 100, SWT.KEYPAD_4 }, { 101, SWT.KEYPAD_5 }, { 102, SWT.KEYPAD_6 }, { 103, SWT.KEYPAD_7 },
      { 104, SWT.KEYPAD_8 }, { 105, SWT.KEYPAD_9 }, { 14, SWT.KEYPAD_CR }, { 107, SWT.KEYPAD_ADD },
      { 109, SWT.KEYPAD_SUBTRACT }, { 106, SWT.KEYPAD_MULTIPLY }, { 111, SWT.KEYPAD_DIVIDE },
      { 110, SWT.KEYPAD_DECIMAL },

      /* Other keys */
      { 20, SWT.CAPS_LOCK }, { 144, SWT.NUM_LOCK }, { 145, SWT.SCROLL_LOCK },
      { 44, SWT.PRINT_SCREEN }, { 6, SWT.HELP }, { 19, SWT.PAUSE }, { 3, SWT.BREAK },

      /* WebKit-specific */
      { 186, ';' }, { 187, '=' }, { 189, '-' }, };

  public class EvaluateFunction extends BrowserFunction {
    public EvaluateFunction(Browser browser, String name) {
      super(browser, name, true, new String[0], false);
    }

    @Override
    public Object function(Object[] arguments) {
      if (arguments[0] instanceof String) {
        String string = (String) arguments[0];
        if (string.startsWith(ERROR_ID)) {
          String errorString = extractError(string);
          if (errorString.length() > 0) {
            evaluateResult = new SWTException(SWT.ERROR_FAILED_EVALUATE, errorString);
          } else {
            evaluateResult = new SWTException(SWT.ERROR_FAILED_EVALUATE);
          }
          return null;
        }
      }
      evaluateResult = arguments[0];
      return null;
    }
  }

  public void addAuthenticationListener(AuthenticationListener listener) {
    AuthenticationListener[] newAuthenticationListeners =
        new AuthenticationListener[authenticationListeners.length + 1];
    System.arraycopy(authenticationListeners, 0, newAuthenticationListeners, 0,
        authenticationListeners.length);
    authenticationListeners = newAuthenticationListeners;
    authenticationListeners[authenticationListeners.length - 1] = listener;
  }

  public void addCloseWindowListener(CloseWindowListener listener) {
    CloseWindowListener[] newCloseWindowListeners =
        new CloseWindowListener[closeWindowListeners.length + 1];
    System.arraycopy(closeWindowListeners, 0, newCloseWindowListeners, 0,
        closeWindowListeners.length);
    closeWindowListeners = newCloseWindowListeners;
    closeWindowListeners[closeWindowListeners.length - 1] = listener;
  }

  public void addLocationListener(LocationListener listener) {
    LocationListener[] newLocationListeners = new LocationListener[locationListeners.length + 1];
    System.arraycopy(locationListeners, 0, newLocationListeners, 0, locationListeners.length);
    locationListeners = newLocationListeners;
    locationListeners[locationListeners.length - 1] = listener;
  }

  public void addOpenWindowListener(OpenWindowListener listener) {
    OpenWindowListener[] newOpenWindowListeners =
        new OpenWindowListener[openWindowListeners.length + 1];
    System.arraycopy(openWindowListeners, 0, newOpenWindowListeners, 0, openWindowListeners.length);
    openWindowListeners = newOpenWindowListeners;
    openWindowListeners[openWindowListeners.length - 1] = listener;
  }

  public void addProgressListener(ProgressListener listener) {
    ProgressListener[] newProgressListeners = new ProgressListener[progressListeners.length + 1];
    System.arraycopy(progressListeners, 0, newProgressListeners, 0, progressListeners.length);
    progressListeners = newProgressListeners;
    progressListeners[progressListeners.length - 1] = listener;
  }

  public void addStatusTextListener(StatusTextListener listener) {
    StatusTextListener[] newStatusTextListeners =
        new StatusTextListener[statusTextListeners.length + 1];
    System.arraycopy(statusTextListeners, 0, newStatusTextListeners, 0, statusTextListeners.length);
    statusTextListeners = newStatusTextListeners;
    statusTextListeners[statusTextListeners.length - 1] = listener;
  }

  public void addTitleListener(TitleListener listener) {
    TitleListener[] newTitleListeners = new TitleListener[titleListeners.length + 1];
    System.arraycopy(titleListeners, 0, newTitleListeners, 0, titleListeners.length);
    titleListeners = newTitleListeners;
    titleListeners[titleListeners.length - 1] = listener;
  }

  public void addVisibilityWindowListener(VisibilityWindowListener listener) {
    VisibilityWindowListener[] newVisibilityWindowListeners =
        new VisibilityWindowListener[visibilityWindowListeners.length + 1];
    System.arraycopy(visibilityWindowListeners, 0, newVisibilityWindowListeners, 0,
        visibilityWindowListeners.length);
    visibilityWindowListeners = newVisibilityWindowListeners;
    visibilityWindowListeners[visibilityWindowListeners.length - 1] = listener;
  }

  public abstract boolean back();

  public static void clearSessions() {
    if (NativeClearSessions != null) {
      NativeClearSessions.run();
    }
  }

  public static String getCookie(String name, String url) {
    CookieName = name;
    CookieUrl = url;
    CookieValue = null;
    if (NativeGetCookie != null) {
      NativeGetCookie.run();
    }
    String result = CookieValue;
    CookieName = CookieValue = CookieUrl = null;
    return result;
  }

  public static boolean setCookie(String value, String url, boolean addToPending) {
    CookieValue = value;
    CookieUrl = url;
    CookieResult = false;
    if (NativeSetCookie != null) {
      NativeSetCookie.run();
    } else {
      if (addToPending && NativePendingCookies != null) {
        NativePendingCookies.add(new String[] { value, url });
      }
    }
    CookieValue = CookieUrl = null;
    return CookieResult;
  }

  static void setPendingCookies(List<String[]> pendingCookies) {
    for (String[] current : pendingCookies) {
      setCookie(current[0], current[1], false);
    }
  }

  public abstract void create(Composite parent, int style);

  static String createErrorString(String error) {
    return ERROR_ID + error;
  }

  static String extractError(String error) {
    return error.substring(ERROR_ID.length());
  }

  public boolean close() {
    return true;
  }

  public void createFunction(BrowserFunction function) {
    /*
     * If an existing function with the same name is found then remove it so that it
     * is not recreated on subsequent pages (the new function overwrites the old
     * one).
     */
    for (BrowserFunction current : functions.values()) {
      if (current.name.equals(function.name)) {
        deregisterFunction(current);
        break;
      }
    }

    function.index = getNextFunctionIndex();
    registerFunction(function);

    StringBuilder functionBuffer = new StringBuilder(function.name);
    functionBuffer.append(" = function "); //$NON-NLS-1$
    functionBuffer.append(function.name);
    functionBuffer.append("() {var result = callJava("); //$NON-NLS-1$
    functionBuffer.append(function.index);
    functionBuffer.append(",'"); //$NON-NLS-1$
    functionBuffer.append(function.token);
    functionBuffer.append(
        "',Array.prototype.slice.call(arguments)); if (typeof result == 'string' &&"
        + " result.indexOf('"); //$NON-NLS-1$
    functionBuffer.append(ERROR_ID);
    functionBuffer.append("') == 0) {var error = new Error(result.substring("); //$NON-NLS-1$
    functionBuffer.append(ERROR_ID.length());
    functionBuffer.append(")); throw error;} return result;};"); //$NON-NLS-1$

    String javaCallDeclaration = getJavaCallDeclaration();

    StringBuilder buffer = new StringBuilder(); //$NON-NLS-1$
    buffer.append(javaCallDeclaration); //$NON-NLS-1$
    if (function.top) {
      buffer.append(functionBuffer.toString());
    }

    buffer.append("var frameIds = null;"); //$NON-NLS-1$
    if (function.frameNames != null) {
      buffer.append("frameIds = {"); //$NON-NLS-1$
      for (int i = 0; i < function.frameNames.length; i++) {
        buffer.append('\'');
        buffer.append(function.frameNames[i]);
        buffer.append("':1,"); //$NON-NLS-1$
      }
      if (function.frameNames.length > 0) {
        buffer.deleteCharAt(buffer.length() - 1);
      }
      buffer.append("};"); //$NON-NLS-1$
    }

    buffer.append(
        "for (var i = 0; i < frames.length; i++) {try {if (!frameIds || (frames[i].name && "
        + "frameIds[frames[i].name])) {"); //$NON-NLS-1$
    buffer.append("if (!frames[i].callJava) {frames[i].callJava = window.callJava;} "
        + "frames[i]."); //$NON-NLS-1$
    buffer.append(functionBuffer.toString());
    buffer.append("}} catch(e) {}};"); //$NON-NLS-1$

    function.functionString = buffer.toString();
    nonBlockingExecute(function.functionString);
  }

  /**
   * Designed to be overriden.
   * @return javaScrit code that defines the 'callJava' syntax for javascript.
   */
  String getJavaCallDeclaration() {
    return "if (!window.callJava) {\n"
        + "   window.callJava = function callJava(index, token, args) {\n"
        + "     return external.callJava(index,token,args);\n" + "    }\n" + "};\n";
  }

  void deregisterFunction(BrowserFunction function) {
    functions.remove(function.index);
  }

  public void destroyFunction(BrowserFunction function) {
    String deleteString = getDeleteFunctionString(function.name);
    StringBuilder buffer =
        new StringBuilder("for (var i = 0; i < frames.length; i++) {try "
            + "{frames[i].eval(\""); //$NON-NLS-1$
    buffer.append(deleteString);
    buffer.append("\");} catch (e) {}}"); //$NON-NLS-1$
    nonBlockingExecute(buffer.toString());
    nonBlockingExecute(deleteString);
    deregisterFunction(function);
  }

  // Designed to be overriden by platform implementations, used for optimization and avoiding
  // deadlocks.
  // Webkit2 is async, we often don't need to bother waiting for a return type if we never use it.
  void nonBlockingExecute(String script) {
    execute(script);
  }

  public abstract boolean execute(String script);

  public Object evaluate(String script, boolean trusted) throws SWTException {
    return evaluate(script);
  }

  public Object evaluate(String script) throws SWTException {
    // Gtk Developer note:
    // Webkit1 uses this mechanism.
    // Webkit2 uses a different mechanism. See WebKit:evaluate();
    BrowserFunction function = new EvaluateFunction(browser, ""); // $NON-NLS-1$
    int index = getNextFunctionIndex();
    function.index = index;
    function.isEvaluate = true; // Note, Webkit2 doesn't use 'isEvaluate' machinery because it
    // doesn't use a function for evaluation.
    registerFunction(function);
    String functionName = EXECUTE_ID + index;

    StringBuilder buffer = new StringBuilder("window."); // $NON-NLS-1$
    buffer.append(functionName);
    buffer.append(" = function "); // $NON-NLS-1$
    buffer.append(functionName);
    buffer.append("() {\n"); // $NON-NLS-1$
    buffer.append(script);
    buffer.append("\n};"); // $NON-NLS-1$
    nonBlockingExecute(buffer.toString());

    buffer = new StringBuilder("if (window."); // $NON-NLS-1$
    buffer.append(functionName);
    buffer.append(" == undefined) {window.external.callJava("); // $NON-NLS-1$
    buffer.append(index);
    buffer.append(",'"); //$NON-NLS-1$
    buffer.append(function.token);
    buffer.append("', ['"); // $NON-NLS-1$
    buffer.append(ERROR_ID);
    buffer.append("']);} else {try {var result = "); // $NON-NLS-1$
    buffer.append(functionName);
    buffer.append("(); window.external.callJava("); // $NON-NLS-1$
    buffer.append(index);
    buffer.append(",'"); //$NON-NLS-1$
    buffer.append(function.token);
    buffer.append("', [result]);} catch (e) {window.external.callJava("); // $NON-NLS-1$
    buffer.append(index);
    buffer.append(",'"); //$NON-NLS-1$
    buffer.append(function.token);
    buffer.append("', ['"); // $NON-NLS-1$
    buffer.append(ERROR_ID);
    buffer.append("' + e.message]);}}"); // $NON-NLS-1$
    nonBlockingExecute(buffer.toString());
    nonBlockingExecute(getDeleteFunctionString(functionName));
    deregisterFunction(function);

    Object result = evaluateResult;
    evaluateResult = null;
    if (result instanceof SWTException) {
      throw (SWTException) result;
    }
    return result;
  }

  public abstract boolean forward();

  public abstract String getBrowserType();

  String getDeleteFunctionString(String functionName) {
    return "delete window." + functionName; //$NON-NLS-1$
  }

  int getNextFunctionIndex() {
    return nextFunctionIndex++;
  }

  public abstract String getText();

  public abstract String getUrl();

  public Object getWebBrowser() {
    return null;
  }

  public abstract boolean isBackEnabled();

  public boolean isFocusControl() {
    return false;
  }

  public abstract boolean isForwardEnabled();

  public abstract void refresh();

  void registerFunction(BrowserFunction function) {
    functions.put(function.index, function);
  }

  public void removeAuthenticationListener(AuthenticationListener listener) {
    if (authenticationListeners.length == 0) {
      return;
    }
    int index = -1;
    for (int i = 0; i < authenticationListeners.length; i++) {
      if (listener == authenticationListeners[i]) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      return;
    }
    if (authenticationListeners.length == 1) {
      authenticationListeners = new AuthenticationListener[0];
      return;
    }
    AuthenticationListener[] newAuthenticationListeners =
        new AuthenticationListener[authenticationListeners.length - 1];
    System.arraycopy(authenticationListeners, 0, newAuthenticationListeners, 0, index);
    System.arraycopy(authenticationListeners, index + 1, newAuthenticationListeners, index,
        authenticationListeners.length - index - 1);
    authenticationListeners = newAuthenticationListeners;
  }

  public void removeCloseWindowListener(CloseWindowListener listener) {
    if (closeWindowListeners.length == 0) {
      return;
    }
    int index = -1;
    for (int i = 0; i < closeWindowListeners.length; i++) {
      if (listener == closeWindowListeners[i]) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      return;
    }
    if (closeWindowListeners.length == 1) {
      closeWindowListeners = new CloseWindowListener[0];
      return;
    }
    CloseWindowListener[] newCloseWindowListeners =
        new CloseWindowListener[closeWindowListeners.length - 1];
    System.arraycopy(closeWindowListeners, 0, newCloseWindowListeners, 0, index);
    System.arraycopy(closeWindowListeners, index + 1, newCloseWindowListeners, index,
        closeWindowListeners.length - index - 1);
    closeWindowListeners = newCloseWindowListeners;
  }

  public void removeLocationListener(LocationListener listener) {
    if (locationListeners.length == 0) {
      return;
    }
    int index = -1;
    for (int i = 0; i < locationListeners.length; i++) {
      if (listener == locationListeners[i]) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      return;
    }
    if (locationListeners.length == 1) {
      locationListeners = new LocationListener[0];
      return;
    }
    LocationListener[] newLocationListeners = new LocationListener[locationListeners.length - 1];
    System.arraycopy(locationListeners, 0, newLocationListeners, 0, index);
    System.arraycopy(locationListeners, index + 1, newLocationListeners, index,
        locationListeners.length - index - 1);
    locationListeners = newLocationListeners;
  }

  public void removeOpenWindowListener(OpenWindowListener listener) {
    if (openWindowListeners.length == 0) {
      return;
    }
    int index = -1;
    for (int i = 0; i < openWindowListeners.length; i++) {
      if (listener == openWindowListeners[i]) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      return;
    }
    if (openWindowListeners.length == 1) {
      openWindowListeners = new OpenWindowListener[0];
      return;
    }
    OpenWindowListener[] newOpenWindowListeners =
        new OpenWindowListener[openWindowListeners.length - 1];
    System.arraycopy(openWindowListeners, 0, newOpenWindowListeners, 0, index);
    System.arraycopy(openWindowListeners, index + 1, newOpenWindowListeners, index,
        openWindowListeners.length - index - 1);
    openWindowListeners = newOpenWindowListeners;
  }

  public void removeProgressListener(ProgressListener listener) {
    if (progressListeners.length == 0) {
      return;
    }
    int index = -1;
    for (int i = 0; i < progressListeners.length; i++) {
      if (listener == progressListeners[i]) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      return;
    }
    if (progressListeners.length == 1) {
      progressListeners = new ProgressListener[0];
      return;
    }
    ProgressListener[] newProgressListeners = new ProgressListener[progressListeners.length - 1];
    System.arraycopy(progressListeners, 0, newProgressListeners, 0, index);
    System.arraycopy(progressListeners, index + 1, newProgressListeners, index,
        progressListeners.length - index - 1);
    progressListeners = newProgressListeners;
  }

  public void removeStatusTextListener(StatusTextListener listener) {
    if (statusTextListeners.length == 0) {
      return;
    }
    int index = -1;
    for (int i = 0; i < statusTextListeners.length; i++) {
      if (listener == statusTextListeners[i]) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      return;
    }
    if (statusTextListeners.length == 1) {
      statusTextListeners = new StatusTextListener[0];
      return;
    }
    StatusTextListener[] newStatusTextListeners =
        new StatusTextListener[statusTextListeners.length - 1];
    System.arraycopy(statusTextListeners, 0, newStatusTextListeners, 0, index);
    System.arraycopy(statusTextListeners, index + 1, newStatusTextListeners, index,
        statusTextListeners.length - index - 1);
    statusTextListeners = newStatusTextListeners;
  }

  public void removeTitleListener(TitleListener listener) {
    if (titleListeners.length == 0) {
      return;
    }
    int index = -1;
    for (int i = 0; i < titleListeners.length; i++) {
      if (listener == titleListeners[i]) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      return;
    }
    if (titleListeners.length == 1) {
      titleListeners = new TitleListener[0];
      return;
    }
    TitleListener[] newTitleListeners = new TitleListener[titleListeners.length - 1];
    System.arraycopy(titleListeners, 0, newTitleListeners, 0, index);
    System.arraycopy(titleListeners, index + 1, newTitleListeners, index,
        titleListeners.length - index - 1);
    titleListeners = newTitleListeners;
  }

  public void removeVisibilityWindowListener(VisibilityWindowListener listener) {
    if (visibilityWindowListeners.length == 0) {
      return;
    }
    int index = -1;
    for (int i = 0; i < visibilityWindowListeners.length; i++) {
      if (listener == visibilityWindowListeners[i]) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      return;
    }
    if (visibilityWindowListeners.length == 1) {
      visibilityWindowListeners = new VisibilityWindowListener[0];
      return;
    }
    VisibilityWindowListener[] newVisibilityWindowListeners =
        new VisibilityWindowListener[visibilityWindowListeners.length - 1];
    System.arraycopy(visibilityWindowListeners, 0, newVisibilityWindowListeners, 0, index);
    System.arraycopy(visibilityWindowListeners, index + 1, newVisibilityWindowListeners, index,
        visibilityWindowListeners.length - index - 1);
    visibilityWindowListeners = newVisibilityWindowListeners;
  }

  boolean sendKeyEvent(Event event) {
    int traversal = SWT.TRAVERSE_NONE;
    boolean traverseDoit = true;
    switch (event.keyCode) {
      case SWT.ESC: {
        traversal = SWT.TRAVERSE_ESCAPE;
        traverseDoit = true;
        break;
      }
      case SWT.CR: {
        traversal = SWT.TRAVERSE_RETURN;
        traverseDoit = false;
        break;
      }
      case SWT.ARROW_DOWN:
      case SWT.ARROW_RIGHT: {
        traversal = SWT.TRAVERSE_ARROW_NEXT;
        traverseDoit = false;
        break;
      }
      case SWT.ARROW_UP:
      case SWT.ARROW_LEFT: {
        traversal = SWT.TRAVERSE_ARROW_PREVIOUS;
        traverseDoit = false;
        break;
      }
      case SWT.TAB: {
        traversal =
            (event.stateMask & SWT.SHIFT) != 0 ? SWT.TRAVERSE_TAB_PREVIOUS : SWT.TRAVERSE_TAB_NEXT;
        traverseDoit = (event.stateMask & SWT.CTRL) != 0;
        break;
      }
      case SWT.PAGE_DOWN: {
        if ((event.stateMask & SWT.CTRL) != 0) {
          traversal = SWT.TRAVERSE_PAGE_NEXT;
          traverseDoit = true;
        }
        break;
      }
      case SWT.PAGE_UP: {
        if ((event.stateMask & SWT.CTRL) != 0) {
          traversal = SWT.TRAVERSE_PAGE_PREVIOUS;
          traverseDoit = true;
        }
        break;
      }
      default: {
        if (translateMnemonics()) {
          if (event.character != 0 && (event.stateMask & (SWT.ALT | SWT.CTRL)) == SWT.ALT) {
            traversal = SWT.TRAVERSE_MNEMONIC;
            traverseDoit = true;
          }
        }
        break;
      }
    }

    boolean doit = true;
    if (traversal != SWT.TRAVERSE_NONE) {
      boolean oldEventDoit = event.doit;
      event.doit = traverseDoit;
      doit = !browser.traverse(traversal, event);
      event.doit = oldEventDoit;
    }
    if (doit) {
      browser.notifyListeners(event.type, event);
      doit = event.doit;
    }
    return doit;
  }

  public void setBrowser(Browser browser) {
    this.browser = browser;
  }

  public abstract boolean setText(String html, boolean trusted);

  public abstract boolean setUrl(String url, String postData, String[] headers);

  public abstract void stop();

  int translateKey(int key) {
    for (int i = 0; i < KeyTable.length; i++) {
      if (KeyTable[i][0] == key) {
        return KeyTable[i][1];
      }
    }
    return 0;
  }

  boolean translateMnemonics() {
    return true;
  }

  public void dispose() {
  }

}
