package io.hotmoka.examples.errors.inconsistentfromcontract1;

import io.takamaka.code.lang.FromContract;

public class Sub extends Super {
	public @FromContract void m() {}
}