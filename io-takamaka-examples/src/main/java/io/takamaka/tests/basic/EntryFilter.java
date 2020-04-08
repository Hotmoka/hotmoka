package io.takamaka.tests.basic;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.ThrowsExceptions;
import io.takamaka.code.lang.View;

public class EntryFilter extends Contract {
	public @Entry @View void foo1() {}
	public @Entry(Contract.class) @View void foo2() {}
	public @Entry(PayableContract.class) @View void foo3() {}
	public @Entry(EntryFilter.class) @View void foo4() {}
	public @ThrowsExceptions @View void foo5() throws MyCheckedException {
		throw new MyCheckedException();
	}
	public @View void foo6() throws MyCheckedException {
		throw new MyCheckedException();
	}
}