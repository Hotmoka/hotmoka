package io.takamaka.tests.basic;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.ThrowsExceptions;

public class EntryFilter extends Contract {
	public @Entry void foo1() {}
	public @Entry(Contract.class) void foo2() {}
	public @Entry(PayableContract.class) void foo3() {}
	public @Entry(EntryFilter.class) void foo4() {}
	public @ThrowsExceptions void foo5() throws MyCheckedException {
		throw new MyCheckedException();
	}
	public void foo6() throws MyCheckedException {
		throw new MyCheckedException();
	}
}