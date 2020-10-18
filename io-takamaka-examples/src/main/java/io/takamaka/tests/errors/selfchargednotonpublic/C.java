package io.takamaka.tests.errors.selfchargednotonpublic;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.SelfCharged;

public class C extends Contract {

	@SelfCharged void foo() {}
}