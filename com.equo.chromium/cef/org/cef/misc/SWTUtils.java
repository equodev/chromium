/****************************************************************************
**
** Copyright (C) 2026 Equo
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
package org.cef.misc;

import java.util.concurrent.Callable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

public class SWTUtils {
	public static final boolean IS_WIN_MULTITHREAD = "win32".equals(SWT.getPlatform())
			&& Boolean.getBoolean("chromium.multi_threaded_message_loop");

	private static class ErrorHandlingCallable<T> implements Runnable {

		private Callable<T> runnable;

		private T result;

		private Exception exception;

		private ErrorHandlingCallable(Callable<T> runnable) {
			super();
			this.runnable = runnable;
		}

		@Override
		public void run() {
			try {
				result = runnable.call();
			} catch (Exception e) {
				exception = e;
			}
		}
	}

	public static void syncExec(Runnable run) {
		if (Display.getCurrent() != null) {
			run.run();
		} else {
			Display.getDefault().syncExec(run);
		}
	}

	public static <V> V syncExec(Callable<V> run) {
		ErrorHandlingCallable<V> handling = new ErrorHandlingCallable<V>(run);
		if (Display.getCurrent() != null) {
			handling.run();
		} else {
			Display.getDefault().syncExec(handling);
		}
		if (handling.exception != null)
			throw new RuntimeException(handling.exception);
		return handling.result;
	}
	
	public static void asyncExec(Runnable run) {
		Display.getDefault().asyncExec(run);
	}

	public static void winMTExec(Runnable runn, boolean immediate) {
		if (IS_WIN_MULTITHREAD) {
			if (immediate) {
				// When using multithreading on Windows, this method is not called from the UI
				// thread and does not support making UI calls from the listener.
				runn.run();
			} else {
				asyncExec(runn);
			}
		} else {
			syncExec(runn);
		}
	}
}
