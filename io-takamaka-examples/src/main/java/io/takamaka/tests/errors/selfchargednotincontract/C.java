package io.takamaka.tests.errors.selfchargednotincontract;

import io.takamaka.code.lang.SelfCharged;
import io.takamaka.code.lang.Storage;

public class C extends Storage {

	public @SelfCharged void foo() {}
}