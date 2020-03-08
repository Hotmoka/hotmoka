package io.takamaka.tests.errors.illegalcalltononwhitelistedmethod2;

import java.util.function.Supplier;

public class C {

	public long foo() {
		return test(System::currentTimeMillis); //// KO: this goes inside a bootstrap method
	}

	private long test(Supplier<Long> supplier) {
		return supplier.get();
	}
}