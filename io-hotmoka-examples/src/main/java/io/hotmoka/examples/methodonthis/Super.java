package io.hotmoka.examples.methodonthis;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.View;

public class Super extends Contract {
	public @FromContract @Payable void foo(int amount) {}

	public @View BigInteger getBalance() {
		return super.balance();
	}
}