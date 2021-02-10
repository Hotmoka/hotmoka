package io.hotmoka.examples.errors.illegalcalltofromcontract2;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C extends Contract {

	public @FromContract void entry() {}
}