package jnr.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.nio.charset.Charset;

import org.junit.Test;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;


public class JNRTest {

	public static interface StructLib {
		int fn_int_no_args();
		String fn_int_int_char_bool_args(int a1, String a2, boolean a3);
		String fn_int_string_array_args(int argc, String[] argv);
		String fn_struct_int(Int_struct st);
		String fn_struct_string_array(String_array_struct st);
	}
	
	public static class Int_struct extends Struct {
		protected Int_struct(Runtime runtime, java.lang.String str) {
			super(runtime);
			a2 = new UTF8StringRef(str.length());
			a2.set(str);
		}
		NumberField a1 = new Signed32();
		//String a2 = new UTFString(4, Charset.defaultCharset());
		//String a2 = new AsciiStringRef(1);
		String a2;
		//String a2 = new AsciiString(4);
	}
	
	public static class String_array_struct extends Struct {
		NumberField a1 = new Signed32();
		String[] a2;
		
		protected String_array_struct(Runtime runtime, java.lang.String[] args) {
			super(runtime);
			a2 = array(args);
			for (int i = 0; i < args.length; i++) {
				//a2[i].set(args[i]);
			}
		}
		
	    protected String[] array(java.lang.String[] args) {
	        arrayBegin();
	        String[] newArgs = new String[args.length];
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
	public void test_fn_struct_string_array() {
		StructLib lib = getLib();
		
		String[] args = new String[] {"ab"/*, "cb", "qwerty azerty"*/};
		String_array_struct st = new String_array_struct(Runtime.getRuntime(lib), args);
		st.a1.set(args.length);
		
		assertThat(lib.fn_struct_string_array(st)).isEqualTo("3:ab_cb_qwerty azerty");
	}
	
	private StructLib getLib() {
		return LibraryLoader.create(StructLib.class).load("jnr.test");
	}
}
