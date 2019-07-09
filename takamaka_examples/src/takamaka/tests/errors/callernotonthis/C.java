package takamaka.tests.errors.callernotonthis;

import takamaka.lang.Contract;
import takamaka.lang.Entry;

public class C extends Contract {
	private C caller;

	public @Entry(C.class) C() {
		this.caller = (C) caller(); // ok
	}

	public void m() {
		caller.caller(); // ko
	}
}