package org.eclipse.swt.internal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;

/**
 * Instances of this class represent entry points into Java
 * which can be invoked from operating system level callback
 * routines.
 * <p>
 * IMPORTANT: A callback is only valid when invoked on the
 * thread which created it. The results are undefined (and
 * typically bad) when a callback is passed out to the
 * operating system (or other code) in such a way that the
 * callback is called from a different thread.
 */

public class CallbackExt extends Callback {

	static final boolean is32Bit = C.PTR_SIZEOF == 4 ? true : false;
    private static Field objectF;
    private static Field methodF;
    private static Field argCountF;
    private static Field isStaticF;
    private static Field signatureF;
    private static Field addressF;
    private static Method bindM;
	
	static {
        try {
            objectF = Callback.class.getDeclaredField("object");
            objectF.setAccessible(true);
            methodF = Callback.class.getDeclaredField("method");
            methodF.setAccessible(true);
            argCountF = Callback.class.getDeclaredField("argCount");
            argCountF.setAccessible(true);
            isStaticF = Callback.class.getDeclaredField("isStatic");
            isStaticF.setAccessible(true);
            signatureF = Callback.class.getDeclaredField("signature");
            signatureF.setAccessible(true);
            addressF = Callback.class.getDeclaredField("address");
            addressF.setAccessible(true);
            bindM = Callback.class.getDeclaredMethod("bind", Callback.class, Object.class, String.class, String.class, int.class, boolean.class, boolean.class, long.class);
            bindM.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException | NoSuchMethodException e) {
            throw new SWTException("Cannot access Callback fields");
        }
	}

/**
 * <p>Register the java method to be a C callback.
 * I.e, C will be able to make a call to this java method directly (through callback.c)</p>
 *
 * <p>The other constructors hard-code int/long into the method signature:<br>
 * <code> long method (long ...) </code><br>
 * Which is suitable for int/long and pointers.<br>
 * This constructor is used if you need to use a different return/argument type, e.g double. See Bug 510538 </p>
 *
 * <p> Note:
 * <ul>
 * <li> Array support is not implemented/supported by this constructor. Use other constructors.</li>
 * <li> If the object is an instance of <code>Class</code> it is assumed that
 * the method is a static method on that class. </li>
 * <li> Note, long types are converted to ints on 32 bit system automatically to account for smaller pointers.
 * This means if you use 'long', you need to cast int next to it. like: <code> long &#47;*int*&#47;</code> </li>
 * </ul></p>
 *
 * <p>The following types are supported: <br>
 * <ul>
 * <li>void (for return values only) </li>
 * <li>int</li>
 * <li>long</li>
 * <li>byte</li>
 * <li>char</li>
 * <li>double</li>
 * <li>float</li>
 * <li>short</li>
 * <li>boolean</li>
 * </ul>
 *
 * <p> For example if you want to link the following method: <br>
 * <code> void myMethod(long &#47;*int*&#47; arg1, double arg2) </code> <br>
 * Then you would call this callback like:<br>
 * <code> Callback (this, "myMethod", void.class, new Type []{long.class, double.class}); </code>
 * </p>
 *
 * @param object the object to send the message to
 * @param method method the name of the method to invoke
 * @param returnType specify the type like  <code>void.class, long.class, double.class </code>
 * @param arguments specify the list of arguments like <code> new Type [] {long.class, double.class } </code>
 */
public CallbackExt (Object object, String method, Type returnType, Type [] arguments) {
    super(CallbackExt.class, "mock", 0, false);
    dispose();
	/* Set the callback fields */
    boolean isStatic = object instanceof Class;
    try {
        objectF.set(this, object);
        methodF.set(this, method);
        argCountF.set(this, arguments != null ? arguments.length : 0);
        isStaticF.set(this, isStatic);
    } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new SWTException(e.getMessage());
    }

	Function <Type, String> getTypeLetter = type -> {
		// https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.2
		if (int.class.equals(type)) {
			return "I";
		} else if (long.class.equals(type)) {
			return "J";
		} else if (void.class.equals(type)) { // for return type.
			return "V";
		} else if (byte.class.equals(type)) {
			return "B";
		} else if (char.class.equals(type)) {
			return "C";
		} else if (double.class.equals(type)) {
			return "D";
		} else if (float.class.equals(type)) {
			return "F";
		} else if (short.class.equals(type)) {
			return "S";
		} else if (boolean.class.equals(type)) {
			return "Z";
		}
		SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, type.toString() + "Not supported");
		return null; // not reachable. Suppress warning.
	};

	StringBuilder signature = new StringBuilder("(");
	for (Type t : arguments) {
		if (t.equals(void.class)) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, "void is not a valid argument");
		}
		signature.append(getTypeLetter.apply(t));
	}
	signature.append(")");
	signature.append(getTypeLetter.apply(returnType));
	try {
        String signatureStr = signature.toString();
        signatureF.set(this, signatureStr);
        if (is32Bit) {
            signatureF.set(this, signatureStr.replace("J", "I"));
        }
    } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new SWTException("Failed to set signature");
    }

	/* Bind the address */
	try {
        long address = (long) bindM.invoke(isStatic ? null : objectF.get(this), this, objectF.get(this), (String)methodF.get(this), (String)signatureF.get(this), (int)argCountF.get(this), (boolean)isStaticF.get(this), false, 0);
        addressF.set(this, address);
    } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
        throw new SWTException("Failed to bind jni");
    }
}

static long /*int*/ mock() {
    throw new SWTException("mock method should never be called");
}
}