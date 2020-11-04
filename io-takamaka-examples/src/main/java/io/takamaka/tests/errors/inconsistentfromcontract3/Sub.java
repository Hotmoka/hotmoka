package io.takamaka.tests.errors.inconsistentfromcontract3;

import io.takamaka.code.lang.FromContract;

public class Sub extends Super {
	public @FromContract(Sub.class) void m() {}
}