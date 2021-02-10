package io.hotmoka.examples.errors.selfchargednotoninstance;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.SelfCharged;

public class C extends Contract {

	public static @SelfCharged void foo() {}
}