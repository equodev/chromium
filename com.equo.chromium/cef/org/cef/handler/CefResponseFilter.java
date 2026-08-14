// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.handler;

import org.cef.misc.IntRef;

/**
 * Implement this interface to handle custom response filters. The methods of
 * this class will always be called on the IO thread.
 */
public abstract class CefResponseFilter {
    /**
     * Some or all of the pre-filter data was read successfully but more data is
     * needed in order to continue filtering (filtered output is pending).
     */
    protected static final int RESPONSE_FILTER_NEED_MORE_DATA = 0;
    /**
     * Some or all of the pre-filter data was read successfully and all available
     * filtered output has been written.
     */
    protected static final int RESPONSE_FILTER_DONE = 1;
    /**
     * An error occurred during filtering.
     */
    protected static final int RESPONSE_FILTER_ERROR = 2;

    /**
     * Begin processing the request.
     */
    public abstract boolean initFilter();

    /**
     * Called to filter a chunk of data. Expected usage is as follows:
     *
     * 1. Read input data from |data_in| and set |data_in_read| to the number of
     * bytes that were read up to a maximum of |data_in_size|. |data_in| will be
     * NULL if |data_in_size| is zero. 2. Write filtered output data to |data_out|
     * and set |data_out_written| to the number of bytes that were written up to a
     * maximum of |data_out_size|. If no output data was written then all data must
     * be read from |data_in| (user must set |data_in_read| = |data_in_size|). 3.
     * Return RESPONSE_FILTER_DONE if all output data was written or
     * RESPONSE_FILTER_NEED_MORE_DATA if output data is still pending.
     *
     * This method will be called repeatedly until the input buffer has been fully
     * read (user sets |data_in_read| = |data_in_size|) and there is no more input
     * data to filter (the resource response is complete). This method may then be
     * called an additional time with an empty input buffer if the user filled the
     * output buffer (set |data_out_written| = |data_out_size|) and returned
     * RESPONSE_FILTER_NEED_MORE_DATA to indicate that output data is still pending.
     *
     * Calls to this method will stop when one of the following conditions is met:
     *
     * 1. There is no more input data to filter (the resource response is complete)
     * and the user sets |data_out_written| = 0 or returns RESPONSE_FILTER_DONE to
     * indicate that all data has been written, or; 2. The user returns
     * RESPONSE_FILTER_ERROR to indicate an error.
     *
     * Do not keep a reference to the buffers passed to this method.
     *
     * @param dataIn         Read input data from |data_in|
     * @param dataInSize     maximum number of bytes that were read
     * @param dataInRead     set |data_in_read| to the number of bytes that were
     *                       read
     * @param dataOut        Write filtered output data to |data_out|
     * @param dataOutSize    maximum number of bytes that were written
     * @param dataOutWritten the number of bytes that were written
     */
    public abstract int filter(byte[] dataIn, int dataInSize, IntRef dataInRead, byte[] dataOut,
            int dataOutSize, IntRef dataOutWritten);
}
