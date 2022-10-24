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

public class CefAppStandalone implements WindowingToolkit {
    @Override
    public CefClient createClient() {
        return new CefClientStandalone();
    }

    @Override
    public boolean isEDT() {
        return false;
    }

    @Override
    public void runInEDT(Runnable r) {
        r.run();
    }

    @Override
    public void runInEDTAndWait(Runnable r) throws InterruptedException, InvocationTargetException {
        r.run();
    }

    @Override
    public void stopMessageLoopTimer() {}

    @Override
    public void startMessageLoopTimer(int ms, Runnable run) {}

    @Override
    public void shutdown(Runnable runnable) {
        runnable.run();
    }
}
