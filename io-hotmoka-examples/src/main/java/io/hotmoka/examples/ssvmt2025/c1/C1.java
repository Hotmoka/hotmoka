package io.hotmoka.examples.ssvmt2025.c1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C1 extends Contract {
	@SuppressWarnings("unused")
	private C1 owner;

	public @FromContract(C1.class) C1() {
		owner = (C1) caller(); // ok
	}
}