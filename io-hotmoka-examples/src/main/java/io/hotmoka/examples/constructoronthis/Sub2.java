package io.hotmoka.examples.constructoronthis;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class Sub2 extends Super2 {

	@FromContract @Payable
	public Sub2(int amount) {
		super();
	}
}