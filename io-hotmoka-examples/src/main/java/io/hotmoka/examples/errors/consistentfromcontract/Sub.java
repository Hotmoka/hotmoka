package io.hotmoka.examples.errors.consistentfromcontract;

import io.takamaka.code.lang.FromContract;

public class Sub extends Super {
	public @FromContract(Super.class) void m() {}
}