package io.hotmoka.examples.errors.illegalcalltofromcontract1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C extends Contract {

	public @FromContract void entry() {}

	public static void m() {
		new C().entry(); // KO
	}
}