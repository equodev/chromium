// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.handler;

import org.cef.misc.IntRef;

public abstract class CefResponseFilterAdapter extends CefResponseFilter {
    @Override
    public boolean initFilter() {
        return true;
    }

    @Override
    public int filter(byte[] dataIn, int dataInSize, IntRef dataInRead, byte[] dataOut,
            int dataOutSize, IntRef dataOutWritten) {
        return 0;
    }
}
