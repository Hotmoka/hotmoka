package io.hotmoka.examples.wtsc2021_c3;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C3 extends Contract {
	private C3 owner;

	public @FromContract(C3.class) C3() {
		owner = (C3) caller(); // ok
	}

	public @FromContract void m() {
		Contract c = owner.caller(); // error at deployment-time
	}
}