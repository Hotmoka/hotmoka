package io.hotmoka.examples.constructoronthis;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.View;

public class Super extends Contract {
	public @FromContract @Payable Super(int amount) {}

	public @View BigInteger getBalance() {
		return super.balance();
	}
}