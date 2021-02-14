package io.hotmoka.examples.errors.illegalcallonthis1;

import io.takamaka.code.lang.FromContract;

public class Sub extends Super {

	// cannot call the constructor of the superclass, since it is on "this" and @Payable
	public @FromContract Sub() {
		super(42);
	}
}