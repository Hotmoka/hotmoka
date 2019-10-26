package takamaka.tests.errors.callernotonthis;

import io.takamaka.code.annotations.Entry;
import io.takamaka.code.lang.Contract;

public class C extends Contract {
	private C caller;

	public @Entry(C.class) C() {
		this.caller = (C) caller(); // ok
	}

	public void m() {
		caller.caller(); // ko
	}
}