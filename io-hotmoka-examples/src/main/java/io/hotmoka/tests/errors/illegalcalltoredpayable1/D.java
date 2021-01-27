package io.hotmoka.tests.errors.illegalcalltoredpayable1;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.RedGreenContract;
import io.takamaka.code.lang.RedPayable;

public class D extends RedGreenContract {
	public @RedPayable @FromContract void foo(int amount) {}
}
