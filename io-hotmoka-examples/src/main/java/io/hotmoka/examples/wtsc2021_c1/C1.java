package io.hotmoka.examples.wtsc2021_c1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C1 extends Contract {
	private C1 owner;

	public @FromContract(C1.class) C1() {
		owner = (C1) caller(); // ok
	}
}