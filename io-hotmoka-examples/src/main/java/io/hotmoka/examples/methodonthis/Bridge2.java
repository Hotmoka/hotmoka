package io.hotmoka.examples.methodonthis;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.View;

public class Bridge2 extends Contract {
	private Sub2 sub;
	private BigInteger initialBalance;

	public @FromContract @Payable void foo(int amount) {
		sub = new Sub2();
		initialBalance = balance();
		sub.foo2(amount);
	}

	public @View BigInteger getBalance() {
		return balance();
	}

	public @View BigInteger getInitialBalance() {
		return initialBalance;
	}

	public @View BigInteger getBalanceOfSub() {
		return sub.getBalance();
	}
}