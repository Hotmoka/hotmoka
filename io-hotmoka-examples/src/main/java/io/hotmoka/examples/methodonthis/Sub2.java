package io.hotmoka.examples.methodonthis;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class Sub2 extends Super2 {

	@FromContract @Payable
	public void foo2(int amount) {
		super.foo();
	}
}