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
package org.cef;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CefAppStandalone implements WindowingToolkit {
    private static final boolean IS_MAC = OS.isMacintosh();

    private static boolean isMultiThreaded() {
        return Boolean.getBoolean("chromium.multi_threaded_message_loop");
    }
    private static volatile Thread CEF_THREAD;
    private static final ExecutorService CEF_EXEC = IS_MAC ? null : Executors.newSingleThreadExecutor(r -> {
        CEF_THREAD = new Thread(r, "cef-standalone");
        CEF_THREAD.setDaemon(false);
        return CEF_THREAD;
    });

    public static void runOnUIThread(Runnable r) {
        if (IS_MAC || Thread.currentThread() == CEF_THREAD) { r.run(); return; }
        CEF_EXEC.execute(r);
    }

    /**
     * Drain any pending CEF work that was queued via runInEDT / startMessageLoopTimer,
     * then tick the macOS NSApp event loop so windows paint and receive input.
     * Intended to be called from the JVM main thread on macOS, where CEF UI work
     * has to run on the same thread as N_CefInitialize.
     *
     * Drains a snapshot of the backlog only — items enqueued during the drain
     * (the pump runnable re-schedules itself) are processed in the next call.
     */
    public static void pumpMessages() {
        if (IS_MAC) {
            // Only pump NSApp once CefApp is live — avoid getInstance() side-effect of constructing a new CefApp.
            CefApp.CefAppState state = CefApp.getState();
            if (state != CefApp.CefAppState.NONE
                    && state != CefApp.CefAppState.TERMINATED
                    && state != CefApp.CefAppState.NEW) {
                CefApp.getInstance().pumpNSApp();
            }
        }
    }

    @Override
    public CefClient createClient() {
        return new CefClientStandalone();
    }

    @Override
    public boolean isEDT() {
        return IS_MAC || Thread.currentThread() == CEF_THREAD;
    }

    @Override
    public void runInEDT(Runnable r) {
        r.run();
    }

    private static boolean shouldRunDirectly() {
        return IS_MAC || !isMultiThreaded() || Thread.currentThread() == CEF_THREAD;
    }

    @Override
    public void runInEDTAndWait(Runnable r) throws InterruptedException, InvocationTargetException {
        if (shouldRunDirectly()) {
            r.run();
            return;
        }
        try {
            CEF_EXEC.submit(r).get();
        } catch (ExecutionException e) {
            throw new InvocationTargetException(e.getCause());
        }
    }

    @Override
    public void stopMessageLoopTimer() {}

    @Override
    public void startMessageLoopTimer(int ms, Runnable run) {}

    @Override
    public void shutdown(Runnable runnable) {
        if (shouldRunDirectly()) {
            runnable.run();
            if (CEF_EXEC != null) {
                CEF_EXEC.shutdown();
            }
            return;
        }
        CEF_EXEC.execute(() -> {
            runnable.run();
            CEF_EXEC.shutdown();
        });
    }
}