package io.takamaka.tests.errors.illegalcalltoredpayable1;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.RedGreenContract;
import io.takamaka.code.lang.RedPayable;

public class D extends RedGreenContract {
	public @RedPayable @Entry void foo(int amount) {}
}
