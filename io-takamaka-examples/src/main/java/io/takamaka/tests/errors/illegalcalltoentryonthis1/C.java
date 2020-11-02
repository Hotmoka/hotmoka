package io.takamaka.tests.errors.illegalcalltoentryonthis1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;

public class C extends Contract {

	public @Entry void entry() {}

	public void m() {
		entry(); // KO
	}
}