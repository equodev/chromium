// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef;

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CefAppSwing implements WindowingToolkit {
    private Timer workTimer_ = null;

    public CefClientSwing createClient() {
        return new CefClientSwing();
    }

    public boolean isEDT() {
        return SwingUtilities.isEventDispatchThread();
    }
    
    public void runInEDT(Runnable r) {
        SwingUtilities.invokeLater(r);
    }

    public void runInEDTAndWait(Runnable r) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(r);
    }

    public void stopMessageLoopTimer() {
        if (workTimer_ != null) {
            workTimer_.stop();
            workTimer_ = null;
        }
    }

    public void startMessageLoopTimer(int ms, Runnable r) {
        workTimer_ = new Timer(ms, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                r.run();
            }
        });
        workTimer_.start();
    }

	@Override
	public void shutdown(Runnable runnable) {
        // Execute on the AWT event dispatching thread. Always call asynchronously
        // so the call stack has a chance to unwind.
        runInEDT(runnable);
	}

}
