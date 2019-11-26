package io.takamaka.tests.errors.illegalmethodname1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;

public class A extends Contract {
	public @Entry boolean foo() {
		Contract caller1 = caller();
		entry(new A()); // sets the caller programmatically!!!!
		Contract caller2 = caller();
		return caller1 == caller2; // callers will be different
	}
}