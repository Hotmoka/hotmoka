package io.hotmoka.examples.constructoronthis;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class Sub extends Super {

	@FromContract @Payable
	public Sub(int amount) {
		super(amount);
	}
}