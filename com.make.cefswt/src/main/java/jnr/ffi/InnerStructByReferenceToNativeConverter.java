package jnr.ffi;

import jnr.ffi.mapper.ToNativeContext;
import jnr.ffi.mapper.ToNativeConverter;

public class InnerStructByReferenceToNativeConverter implements ToNativeConverter<InnerStruct, Pointer> {

    public InnerStructByReferenceToNativeConverter() {
    }

	public Class<Pointer> nativeType() {
        return Pointer.class;
    }

    public Pointer toNative(InnerStruct value, ToNativeContext ctx) {
    	if (value == null)
    		return null;
        Pointer memory = Struct.getMemory(value, 0);
		if (value.__info.enclosing != null) {
			int offset = value.__info.getOffset();
        	Pointer innerMemory = memory.slice(offset);
        	return innerMemory;
        }
		return memory;
    }

}
