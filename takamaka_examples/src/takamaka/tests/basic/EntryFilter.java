package takamaka.tests.basic;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.PayableContract;
import takamaka.lang.ThrowsExceptions;

public class EntryFilter extends Contract {
	public @Entry void foo1() {}
	public @Entry(Contract.class) void foo2() {}
	public @Entry(PayableContract.class) void foo3() {}
	public @Entry(EntryFilter.class) void foo4() {}
	public @Entry(EntryFilter.class) @ThrowsExceptions void foo5() {}
}