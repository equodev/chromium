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

import org.eclipse.swt.internal.SWTEventListener;

/**
 * This listener interface may be implemented in order to receive a
 * {@link WindowEvent} notification when a new {@link Browser} needs to be
 * provided by the application.
 * @see   Browser#addOpenWindowListener(OpenWindowListener)
 * @see   Browser#removeOpenWindowListener(OpenWindowListener)
 * @see   CloseWindowListener
 * @see   VisibilityWindowListener
 * @since 3.0
 */
@FunctionalInterface
public interface OpenWindowListener extends SWTEventListener {

  /**
   * This method is called when a new window needs to be created.
   * 
   * <p>A particular <code>Browser</code> can be passed to the event.browser field to
   * host the content of a new window.
   * 
   * <p>A standalone system browser is used to host the new window if the
   * event.required field value is <code>false</code> and if the event.browser
   * field is left <code>null</code>. The event.required field is
   * <code>true</code> on platforms that don't support a standalone system browser
   * for new window requests.
   * 
   * <p>The navigation is cancelled if the event.required field is set to
   * <code>true</code> and the event.browser field is left <code>null</code>.
   * </p>
   * 
   * <p>The following fields in the <code>WindowEvent</code> apply:
   * <ul>
   * <li>(in/out) {@link WindowEvent#required}: true if the platform requires the
   * user to provide a <code>Browser</code> to handle the new window or false
   * otherwise.
   * <li>(out) {@link WindowEvent#browser}: the new (unique) <code>Browser</code>
   * that will host the content of the new window.
   * <li>(in) widget the <code>Browser</code> that is requesting to open a new
   * window
   * </ul>
   * @param event the <code>WindowEvent</code> that needs to be passed a new
   *              <code>Browser</code> to handle the new window request
   * @since       3.0
   */
  public void open(WindowEvent event);
}
