package io.takamaka.tests.errors.illegalcalltoentry1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;

public class C extends Contract {

	public @Entry void entry() {}

	public static void m() {
		new C().entry(); // KO
	}
}