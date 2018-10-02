package org.eclipse.swt.internal.chromium;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;

import org.eclipse.swt.internal.chromium.CEFFactory.cef_base_ref_counted_t.add_ref;
import org.eclipse.swt.internal.chromium.CEFFactory.cef_base_ref_counted_t.has_one_ref;
import org.eclipse.swt.internal.chromium.CEFFactory.cef_base_ref_counted_t.release;

//import jnr.ffi.InnerStructByReferenceToNativeConverter;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.annotations.Delegate;
import jnr.ffi.mapper.CompositeTypeMapper;
import jnr.ffi.mapper.DefaultTypeMapper;
import jnr.ffi.mapper.FromNativeContext;
import jnr.ffi.mapper.SignatureTypeMapper;
import jnr.ffi.mapper.SignatureTypeMapperAdapter;
import jnr.ffi.provider.ClosureManager;
import jnr.ffi.provider.ParameterFlags;
import jnr.ffi.provider.converters.StructByReferenceFromNativeConverter;
import jnr.ffi.provider.jffi.NativeRuntime;

public class CEFFactory {

	public static Runtime RUNTIME;
	private static DefaultTypeMapper typeMapper;
	private static final FromNativeContext closureToNativeContext = new FromNativeContext() {
		
		@Override
		public jnr.ffi.Runtime getRuntime() {
			return CEFFactory.RUNTIME;
		}
		
		@Override
		public Collection<Annotation> getAnnotations() {
			return null;
		}
	};
	
	public interface Lib {
		
	}

	public static Lib create() {
		boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
		boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
		String toLoad = "cef";
		String cefrustPath = System.getProperty("cefswt.path", "");
		if (isWin) {
			toLoad = "libcef";
		} else if (isMac) {
			toLoad = cefrustPath + "/Chromium Embedded Framework.framework/Chromium Embedded Framework";
		} else {
			toLoad = cefrustPath + "/libcef.so";
		}

		Lib lib = LibraryLoader.create(Lib.class)
//				.map(StringUtf8.class, new InnerStructByReferenceToNativeConverter())
//				.map(StringUtf16.class, new InnerStructByReferenceToNativeConverter())
				.load(toLoad);
		RUNTIME = jnr.ffi.Runtime.getRuntime(lib);
		
		return lib;
	}
	
	private static DefaultTypeMapper getClosureTypeMapper() {
		try {
			if (typeMapper == null) {
				ClosureManager closureManager = NativeRuntime.getInstance().getClosureManager();
				Field compTypeMapperField = closureManager.getClass().getDeclaredField("typeMapper");
				compTypeMapperField.setAccessible(true);
				CompositeTypeMapper compTypeMapper = (CompositeTypeMapper) compTypeMapperField.get(closureManager);
				Field signatureTypeMappersField = CompositeTypeMapper.class.getDeclaredField("signatureTypeMappers");
				signatureTypeMappersField.setAccessible(true);
				@SuppressWarnings("unchecked")
				Collection<SignatureTypeMapper> signatureTypeMappers = (Collection<SignatureTypeMapper>) signatureTypeMappersField.get(compTypeMapper);
				SignatureTypeMapper signatureTypeMapper = signatureTypeMappers.iterator().next();
				Field typeMapperField = SignatureTypeMapperAdapter.class.getDeclaredField("typeMapper");
				typeMapperField.setAccessible(true);
				typeMapper = (DefaultTypeMapper) typeMapperField.get(signatureTypeMapper);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return typeMapper;
	}

	public static void mapTypeForClosure(Class<? extends Struct> structClass) {
		DefaultTypeMapper typeMapper = getClosureTypeMapper();
		if (typeMapper != null) {
			typeMapper.put(structClass, StructByReferenceFromNativeConverter.getInstance(structClass, closureToNativeContext));
		}
	}

	public static CEF.cef_app_t newApp() {
	    mapTypeForClosure(CEF.cef_string_utf16_t.class);
	    mapTypeForClosure(CEF.cef_string_t.class);
	    mapTypeForClosure(CEF.cef_popup_features_t.class);
	    mapTypeForClosure(CEF.cef_cookie_t.class);
		CEF.cef_app_t st = new CEF.cef_app_t(RUNTIME);
		setBase(st, st.base);
		return st;
	}
	
	public static CEF.cef_browser_process_handler_t newBrowserProcessHandler() {
		CEF.cef_browser_process_handler_t st = new CEF.cef_browser_process_handler_t(RUNTIME);
		setBase(st, st.base);
		return st;
	}
	
	public static CEF.cef_client_t newClient() {
		CEF.cef_client_t st = new CEF.cef_client_t(RUNTIME);
		setBase(st, st.base);
		return st;
	}
	
	public static CEF.cef_focus_handler_t newFocusHandler() {
		CEF.cef_focus_handler_t st = new CEF.cef_focus_handler_t(RUNTIME);
		setBase(st, st.base);
		return st;
	}
	
	public static CEF.cef_life_span_handler_t newLifeSpanHandler() {
		CEF.cef_life_span_handler_t st = new CEF.cef_life_span_handler_t(RUNTIME);
		setBase(st, st.base);
		return st;
	}

	public static CEF.cef_load_handler_t newLoadHandler() {
	    CEF.cef_load_handler_t st = new CEF.cef_load_handler_t(RUNTIME);
	    setBase(st, st.base);
	    return st;
	}
	
	public static CEF.cef_display_handler_t newDisplayHandler() {
	    CEF.cef_display_handler_t st = new CEF.cef_display_handler_t(RUNTIME);
	    setBase(st, st.base);
	    return st;
	}
	
	public static CEF.cef_string_visitor_t newStringVisitor() {
	    CEF.cef_string_visitor_t st = new CEF.cef_string_visitor_t(RUNTIME);
	    setBase(st, st.base);
	    return st;
	}
	
	public static CEF.cef_cookie_visitor_t newCookieVisitor() {
	    CEF.cef_cookie_visitor_t st = new CEF.cef_cookie_visitor_t(RUNTIME);
	    setBase(st, st.base);
	    return st;
	}
	
    private static void setBase(Struct st, cef_base_ref_counted_t base) {
		directMemoryForStruct(st);
		setBaseRefCounting(st, base);
		setBaseSize(st, base);
	}

	private static void setBaseSize(Struct st, cef_base_ref_counted_t base) {
		int sizeof = Struct.size(st);
//		System.out.println("J:SIZEOF:" + st.getClass().getSimpleName() + ":" + sizeof);
		base.size.set(sizeof);
		base.name = st.getClass().getSimpleName();
	}

	private static void setBaseRefCounting(Struct st, cef_base_ref_counted_t base) {
		base.add_ref.set(new AddRefFN(base));
		base.release.set(new ReleaseFN(base));
		base.has_one_ref.set(new HasOneRefFN(base));
	}

	/**
	 * This is required to return struct in callback and to create native
	 * memory.
	 *
	 * @param struct
	 */
	static void directMemoryForStruct(Struct struct) {
		Struct.getMemory(struct, ParameterFlags.DIRECT);
	}

	///
	/// All ref-counted framework structures must include this structure first.
	///
	public static class cef_base_ref_counted_t extends Struct {
		static {
			mapTypeForClosure(cef_base_ref_counted_t.class);
		}
		///
		/// Size of the data structure.
		///
		public UnsignedLong size = new UnsignedLong();
		///
		/// Called to increment the reference count for the object. Should be
		/// called
		/// for every new copy of a pointer to a given object.
		///
		public Function<add_ref> add_ref = function(add_ref.class);
		///
		/// Called to decrement the reference count for the object. If the
		/// reference
		/// count falls to 0 the object should self-delete. Returns true (1) if
		/// the
		/// resulting reference count is 0.
		///
		public Function<release> release = function(release.class);
		///
		/// Returns true (1) if the current reference count is 1.
		///
		public Function<has_one_ref> has_one_ref = function(has_one_ref.class);
		
		int ref = 1;
		public java.lang.String name;

		public static interface add_ref {
			@Delegate
			void invoke(cef_base_ref_counted_t self_);
		}

		public static interface release {
			@Delegate
			int invoke(cef_base_ref_counted_t self_);
		}

		public static interface has_one_ref {
			@Delegate
			int invoke(cef_base_ref_counted_t self_);
		}

		public cef_base_ref_counted_t(jnr.ffi.Runtime runtime) {
			super(runtime);
		}
	}

	public static class AddRefFN implements add_ref {
		private cef_base_ref_counted_t base;

		public AddRefFN(cef_base_ref_counted_t base) {
			this.base = base;
		}

		@Override
		@Delegate
		public void invoke(cef_base_ref_counted_t self) {
			base.ref++;
			// System.out.print("J:+ " + base.name + " ");
			// System.out.println(base.ref);
		}
	}

	// See https://bitbucket.org/chromiumembedded/cef/wiki/UsingTheCAPI.md
	public static class ReleaseFN implements release {
		private cef_base_ref_counted_t base;

		public ReleaseFN(cef_base_ref_counted_t base) {
			this.base = base;
		}

		@Override
		public int invoke(cef_base_ref_counted_t self) {
			base.ref--;
			// System.out.print("J:- " + base.name + " ");
			// System.out.println(base.ref);
			if (base.ref == 0) {
				// TODO: free object using MemoryIO
//				System.out.println("J:" + base.name + " Remove myself");
			}
			return (base.ref == 0) ? 1 : 0;
		}
	}

	public static class HasOneRefFN implements has_one_ref {
		private cef_base_ref_counted_t base;

		public HasOneRefFN(cef_base_ref_counted_t base) {
			this.base = base;
		}

		@Override
		public int invoke(cef_base_ref_counted_t self) {
			// System.out.print("J:= " + base.name + " ");
			// System.out.println(base.ref);
			return base.ref == 1 ? 1 : 0;
		}
	}

}
