package io.takamaka.tests.errors.inconsistentfromcontract3;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class Super extends Contract {
	public @FromContract(Super.class) void m() {}
}