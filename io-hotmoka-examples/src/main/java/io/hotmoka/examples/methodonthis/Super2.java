package io.hotmoka.examples.methodonthis;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;

public class Super2 extends Contract {
	public @FromContract void foo() {}

	public @View BigInteger getBalance() {
		return super.balance();
	}
}