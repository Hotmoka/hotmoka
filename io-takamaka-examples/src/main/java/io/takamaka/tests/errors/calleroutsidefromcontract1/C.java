package io.takamaka.tests.errors.calleroutsidefromcontract1;

import io.takamaka.code.lang.Contract;

public class C extends Contract {
	public void m() {
		caller();
	}
}