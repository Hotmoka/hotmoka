package io.hotmoka.examples.methodonthis;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class Sub extends Super {

	@Override @FromContract @Payable
	public void foo(int amount) {
		super.foo(amount);
	}
}