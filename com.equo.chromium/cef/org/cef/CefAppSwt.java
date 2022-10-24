/****************************************************************************
**
** Copyright (C) 2022 Equo
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


package org.cef;

import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.widgets.Display;

public class CefAppSwt implements WindowingToolkit {

	private Timer timer;
	private Runnable loop;
	private Display display =  Display.getDefault();
	private boolean enabled = true;
	private boolean external_message_pump;
	private int loopTime;

	private ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "chromium-EDT");
		thread.setDaemon(true);
		return thread;
	});

	public CefAppSwt(int loopTime, boolean external_message_pump) {
		this.loopTime = loopTime;
		this.external_message_pump = external_message_pump;
	}

	public CefAppSwt(boolean external_message_pump) {
		this(WindowingToolkit.DEFAULT_LOOP_TIME, external_message_pump);
	}

	@Override
	public CefClient createClient() {
		return new CefClientSwt();
	}

	@Override
	public boolean isEDT() {
		return Display.getCurrent() != null;
	}

	@Override
	public void runInEDT(Runnable r) {
		executor.execute(() -> {
			synchronized (Device.class) {
				if (!display.isDisposed())
					display.asyncExec(r);
			}
		});
	}

	@Override
	public void runInEDTAndWait(Runnable r) throws InterruptedException, InvocationTargetException {
		display.syncExec(r);
	}

	@Override
	public void stopMessageLoopTimer() {
		if (loop != null && !display.isDisposed()) {
			display.timerExec(-1, loop);
			loop = null;
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@Override
	public void startMessageLoopTimer(int ms, Runnable run) {
		if (external_message_pump) {
			if (!display.isDisposed()) {
				this.loop = run;
				display.timerExec(ms, run);
			}
		} else if (timer == null && ms != -1) {
			timer = new Timer(true);
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					if (!display.isDisposed())
						display.syncExec(run);
				}
			};
			timer.scheduleAtFixedRate(task, ms, ms);
		} else if (ms == -1) {
			if (!display.isDisposed())
				display.syncExec(run);
		}
	}

	@Override
	public void shutdown(Runnable runnable) {
		executor.shutdown();
		runnable.run();
	}

	public void toggle() {
		enabled  = !enabled;
	}

	@Override
	public int getLoopTime() {
		return loopTime;
	}
}
