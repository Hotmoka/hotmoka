package io.takamaka.tests.errors.consistentfromcontract;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class Super extends Contract {
	public @FromContract(Sub.class) void m() {}
}