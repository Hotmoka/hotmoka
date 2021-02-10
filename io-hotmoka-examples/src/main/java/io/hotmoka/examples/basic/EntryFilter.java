package io.hotmoka.examples.basic;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.ThrowsExceptions;
import io.takamaka.code.lang.View;

public class EntryFilter extends Contract {
	public @FromContract @View void foo1() {}
	public @FromContract(Contract.class) @View void foo2() {}
	public @FromContract(PayableContract.class) @View void foo3() {}
	public @FromContract(EntryFilter.class) @View void foo4() {}
	public @ThrowsExceptions @View void foo5() throws MyCheckedException {
		throw new MyCheckedException();
	}
	public @View void foo6() throws MyCheckedException {
		throw new MyCheckedException();
	}
}