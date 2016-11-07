package jnr.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;

import org.assertj.core.api.AbstractIntegerAssert;
import org.junit.Ignore;
import org.junit.Test;

import cef.capi.CEF;
import cef.capi.CEF.App;
import cef.capi.CEF.MainArgs;
import cef.capi.CEF.Settings;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Memory;
import jnr.ffi.ObjectReferenceManager;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.annotations.Delegate;


public class JNRTest {

	public static interface StructLib {
		int fn_int_no_args();
		String fn_int_int_char_bool_args(int a1, String a2, boolean a3);
		String fn_int_string_array_args(int argc, String[] argv);
		String fn_struct_int(Int_struct st);
		String fn_struct_string_array(String_array_struct st);
		String fn_struct_string_array_fixed(String_array_struct_fixed st);
		String fn_mainargs(MainArgs args, Pointer windows_sandbox_info);
		String fn_settings(Settings settings, Pointer windows_sandbox_info);
		String fn_app(App application, Pointer windows_sandbox_info);
		String fn_callback(StructCallback struct);
		String fn_callback_args(StructCallback struct, int arg);
		String fn_base_refs(CEF.Base base);
		String fn_app_refs(CEF.App app);
	}
	
	public static class Int_struct extends Struct {
		protected Int_struct(Runtime runtime, java.lang.String str) {
			super(runtime);
			a2 = new UTF8StringRef(str.length());
			a2.set(str);
		}
		NumberField a1 = new Signed32();
		String a2;
	}
	
	public static class String_array_struct_fixed extends Struct {
		NumberField a1 = new Signed32();
		Signed32[] a2 = array(new Signed32[2]);
		String[] a3;
		
		protected String_array_struct_fixed(Runtime runtime, java.lang.String[] args) {
			super(runtime);
			a3 = array(args);
		}
		
		protected String[] array(java.lang.String[] args) {
			arrayBegin();
			String[] newArgs = new UTF8StringRef[args.length];
			try {
				for (int i = 0; i < args.length; i++) {
					newArgs[i] = new UTF8StringRef(args[i].length());
				}
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			arrayEnd();
			return newArgs;
		}
	}
	
	public static class String_array_struct extends Struct {
		NumberField a1 = new Signed32();
		Pointer/* int* */ a2 = new Pointer();
		Pointer/* char** */ a3 = new Pointer();
		
		protected String_array_struct(Runtime runtime/*, java.lang.String[] args*/) {
			super(runtime);
		}
	}
	
	@Test
	public void test_fn_int_no_args() {
		StructLib lib = getLib();
		
		assertThat(lib.fn_int_no_args()).isEqualTo(0);
	}

	@Test
	public void test_fn_int_int_char_bool_args() {
		StructLib lib = getLib();
		
		assertThat(lib.fn_int_int_char_bool_args(3, "be", true)).isEqualTo("3_be_true");
	}
	
	@Test
	public void test_fn_int_string_array_args() {
		StructLib lib = getLib();
		
		String[] args = new String[] {"arg1", "--arg2", "arg3"};
		assertThat(lib.fn_int_string_array_args(args.length, args)).isEqualTo("arg1--arg2arg3");
	}
	
	@Test
	public void test_fn_int_struct_int() {
		StructLib lib = getLib();
		
		java.lang.String value = "abcd ef";
		Int_struct st = new Int_struct(Runtime.getRuntime(lib), value);
		st.a1.set(2);
//		st.a2.set(value);
		assertThat(lib.fn_struct_int(st)).isEqualTo("2_" + value);
	}

	@Test
	public void test_fn_struct_string_array_fixed() {
		StructLib lib = getLib();
		Runtime runtime = Runtime.getRuntime(lib);
		
		String[] args = new String[] {"ab", "cb", "qwerty azerty"};
		
		String_array_struct_fixed st = new String_array_struct_fixed(runtime, args);
		st.a1.set(args.length);
		st.a2[0].set(3);
		st.a2[1].set(5);
		
		st.a3[0].set(args[0]);
		st.a3[1].set(args[1]);
		st.a3[2].set(args[2]);
		
		assertThat(lib.fn_struct_string_array_fixed(st)).isEqualTo("3:_ab_cb_qwerty azerty");
	}
	
	@Test
	public void test_fn_struct_string_array() {
		StructLib lib = getLib();
		Runtime runtime = Runtime.getRuntime(lib);
		
		String[] args = new String[] {"ab", "cb", "qwerty azerty"};
		
		
		String_array_struct st = new String_array_struct(runtime);
		st.a1.set(args.length);
		
		int[] ints = new int[] {3, 5};
		jnr.ffi.Pointer intsp = Memory.allocateDirect(runtime, ints.length * (Integer.SIZE / 8));
		intsp.put(0, ints, 0, 2);
		st.a2.set(intsp);
		
		jnr.ffi.Pointer stringp = getStringArrayPointer(runtime, args);
		st.a3.set(stringp);
		
		assertThat(lib.fn_struct_string_array(st)).isEqualTo("3:_ab_cb_qwerty azerty");
	}

	@Test
	public void test_fn_mainargs() {
		StructLib lib = getLib();
		Runtime runtime = Runtime.getRuntime(lib);
		
		String[] d = new String[] {"123", "abcd", "la la"};
		
		MainArgs args = new MainArgs(runtime);
		args.argc.set(d.length);
		args.argv.set(getStringArrayPointer(runtime, d));
		
		assertThat(lib.fn_mainargs(args, null)).isEqualTo("3:_123_abcd_la la");
	}
	
	@Test
	public void test_fn_settings() {
		StructLib lib = getLib();
		Runtime runtime = Runtime.getRuntime(lib);
		
		Settings settings = new Settings(runtime);
		
		assertThat(lib.fn_settings(settings, null)).isEqualTo("ok");
	}
	
	@Test
	public void test_fn_app() {
		StructLib lib = getLib();
		Runtime runtime = Runtime.getRuntime(lib);
		
		App app = new App(runtime);
		
		assertThat(lib.fn_app(app, null)).isEqualTo("ok");
	}
	
	public static class StructCallback extends Struct {
		Signed32 id = new Signed32();
		Function<CallbackFN> callback = function(CallbackFN.class);
		Function<CallbackArgsFN> callbackArgs = function(CallbackArgsFN.class);

		protected StructCallback(Runtime runtime, CallbackFN callbackFN) {
			super(runtime);
			callback.set(callbackFN);
		}
		protected StructCallback(Runtime runtime) {
			super(runtime);
		}
		public void setCallbackArgs(CallbackArgsFN callbackFN) {
			callbackArgs.set(callbackFN);
		}
		
	}
	
	public static class CallbackFN {
		boolean called = false;
		
		@Delegate
		public void called() {
			System.out.println("CallbackFN called");
			called = true;
		}
	}
	
	public static class CallbackArgsFN {
		boolean called = false;
		Pointer st;
		int arg;

		@Delegate
		public int called(Pointer st, int arg) {
			System.out.println("CallbackArgsFN called");
			called = true;
			this.st = st;
			this.arg = arg;
			return arg;
		}
	}
	
	@Test
	public void test_fn_callback() {
		StructLib lib = getLib();
		Runtime runtime = Runtime.getRuntime(lib);

		CallbackFN callbackFN = new CallbackFN();
		StructCallback struct = new StructCallback(runtime, callbackFN);
		struct.id.set(4);
		
		assertThat(lib.fn_callback(struct)).isEqualTo("ok_4");
		assertThat(callbackFN.called).isTrue();
	}

	@Test
	public void test_fn_callback_args() {
		StructLib lib = getLib();
		Runtime runtime = Runtime.getRuntime(lib);
		
		ObjectReferenceManager<Object> objRef = runtime.newObjectReferenceManager();
		CallbackArgsFN callbackFN = new CallbackArgsFN();
		StructCallback struct = new StructCallback(runtime);
//		jnr.ffi.Pointer stPt = Struct.getMemory(struct);
//		jnr.ffi.Pointer stPt1 = objRef.add(struct);
//		assertThat(stPt).isNotNull();
//		assertThat(stPt1).isNotNull();
		struct.id.set(3);
		struct.setCallbackArgs(callbackFN);
		
		assertThat(lib.fn_callback_args(struct, 25)).isEqualTo("ok_3_25");
		assertThat(callbackFN.called).isTrue();
		assertThat(callbackFN.arg).isEqualTo(25);
		
//		assertThat(callbackFN.st).isEqualTo(stPt1);
//		assertThat(objRef.get(callbackFN.st)).isNotNull();
//		assertThat(objRef.get(callbackFN.st)).isEqualTo(struct);
//		assertThat(objRef.get(callbackFN.st)).isSameAs(struct);
	}
	
	@Test
	public void test_fn_base_refs() {
		StructLib lib = getLib();
		Runtime runtime = Runtime.getRuntime(lib);
		
		CEF.Base base  = new CEF.Base(runtime);
		base.size.set(55);
		assertThat(base.ref).isEqualTo(0);
		
		assertThat(lib.fn_base_refs(base)).isEqualTo("ok");
		assertThat(base.ref).isEqualTo(1);
	}
	
	@Test
	public void test_fn_app_refs() {
		StructLib lib = getLib();
		Runtime runtime = Runtime.getRuntime(lib);

		App app = new App(runtime);
		app.base.size.set(22);
		assertThat(app.base).isNotNull();
		assertThat(app.base.add_ref).isNotNull();
		assertThat(app.base.ref).isEqualTo(0);
		
		assertThat(lib.fn_app_refs(app)).isEqualTo("ok");
		assertThat(app.base.ref).isEqualTo(1);
	}
	
	public jnr.ffi.Pointer getStringArrayPointer(Runtime runtime, String[] args) {
		Pointer[] array = new Pointer[args.length];
		for (int i = 0; i < array.length; i++) {
			array[i] = Memory.allocateDirect(runtime, args[i].length());
			array[i].putString(0, args[i], args[i].length(), Charset.defaultCharset());
		}
//        Pointer memory = Memory.allocateDirect(runtime, (2 * array.length + 1) * runtime.addressSize(), true);
//        memory.put(array.length * runtime.addressSize(), array, 0, array.length);
		
		jnr.ffi.Pointer stringp = Memory.allocateDirect(runtime, array.length * (runtime.addressSize()));
		stringp.put(0, array, 0, array.length);
		return stringp;
	}
	
	private StructLib getLib() {
		return LibraryLoader.create(StructLib.class).load("jnr.test");
	}
}
