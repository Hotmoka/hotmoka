package io.hotmoka.examples.constructoronthis;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.View;

public class Super2 extends Contract {
	private final BigInteger initialBalance;

	public Super2() {
		initialBalance = balance();
	}

	public @View BigInteger getBalance() {
		return super.balance();
	}

	public @View BigInteger getInitialBalance() {
		return initialBalance;
	}
}