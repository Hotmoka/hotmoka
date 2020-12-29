package io.takamaka.tests.errors.illegalcalltofromcontractonthis1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C extends Contract {

	public @FromContract void entry() {}

	public void m() {
		entry(); // KO
	}
}