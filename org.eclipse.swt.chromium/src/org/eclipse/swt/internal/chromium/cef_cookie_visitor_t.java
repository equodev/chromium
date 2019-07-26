package org.eclipse.swt.internal.chromium;

import org.eclipse.swt.internal.Callback;

///
/// Structure to implement for visiting cookie values. The functions of this
/// structure will always be called on the IO thread.
///
public class cef_cookie_visitor_t {
    ///
    /// Base structure.
    ///
    public cef_base_ref_counted_t base;
    ///
    /// Method that will be called once for each cookie. |count| is the 0-based
    /// index for the current cookie. |total| is the total number of cookies. Set
    /// |deleteCookie| to true (1) to delete the cookie currently being visited.
    /// Return false (0) to stop visiting cookies. This function may never be
    /// called if no cookies are found.
    ///
    /** @field cast=(void*) */
    public long visit;

    /** @field flags=no_gen */
    public long ptr;
    /** @field flags=no_gen */
	public Callback visit_cb;

    public static final int sizeof = ChromiumLib.cef_cookie_visitor_t_sizeof();
  }