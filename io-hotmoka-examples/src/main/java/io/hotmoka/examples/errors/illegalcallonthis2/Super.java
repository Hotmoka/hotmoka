package io.hotmoka.examples.errors.illegalcallonthis2;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.RedPayable;

public class Super extends Contract {
	public @FromContract @RedPayable Super(int amount) {}
}