package org.eclipse.swt.internal.chromium.lib;

///
/// All ref-counted framework structures must include this structure first.
///
public class cef_base_ref_counted_t {
	///
	/// Size of the data structure.
	///
	/** @field cast=(size_t) */
	public int size;
	///
	/// Called to increment the reference count for the object. Should be
	/// called
	/// for every new copy of a pointer to a given object.
	///
	/** @field cast=(void*) */
	public long /*int*/ add_ref;
	///
	/// Called to decrement the reference count for the object. If the
	/// reference
	/// count falls to 0 the object should self-delete. Returns true (1) if
	/// the
	/// resulting reference count is 0.
	///
	/** @field cast=(void*) */
	public long /*int*/ release;
	///
	/// Returns true (1) if the current reference count is 1.
	///
	/** @field cast=(void*) */
	public long /*int*/ has_one_ref;
	///
	/// Returns true (1) if the current reference count is at least 1.
	///
	/** @field cast=(void*) */
	public long /*int*/ has_at_least_one_ref;

}