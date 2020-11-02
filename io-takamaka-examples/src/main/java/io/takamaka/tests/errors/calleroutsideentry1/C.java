package io.takamaka.tests.errors.calleroutsideentry1;

import io.takamaka.code.lang.Contract;

public class C extends Contract {
	public void m() {
		caller();
	}
}