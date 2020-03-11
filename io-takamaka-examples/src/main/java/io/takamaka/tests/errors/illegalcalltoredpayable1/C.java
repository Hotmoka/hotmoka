package io.takamaka.tests.errors.illegalcalltoredpayable1;

import io.takamaka.code.lang.Contract;

public class C extends Contract {

	public void m() {
		new D().foo(13); // KO
	}
}