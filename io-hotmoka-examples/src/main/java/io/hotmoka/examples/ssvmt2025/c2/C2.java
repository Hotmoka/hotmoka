package io.hotmoka.examples.ssvmt2025.c2;

import io.takamaka.code.lang.Contract;

public class C2 extends Contract {
	@SuppressWarnings("unused")
	private C2 owner;

	public void m() {
		owner = (C2) caller(); // error at deployment time
	}
}