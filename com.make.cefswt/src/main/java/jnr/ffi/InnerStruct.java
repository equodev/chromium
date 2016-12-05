package jnr.ffi;

public abstract class InnerStruct extends Struct {

	public InnerStruct(Runtime runtime) {
		super(runtime);
	}
	
	protected InnerStruct(Runtime runtime, Struct enclosing) {
		super(runtime, enclosing);
	}

}
