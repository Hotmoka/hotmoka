package io.hotmoka.examples.errors.calleronthis;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C extends Contract {
	@SuppressWarnings("unused")
	private C owner;

	public @FromContract(C.class) C() {
		this.owner = (C) caller(); // ok
	}
}