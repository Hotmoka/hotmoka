package io.hotmoka.examples.errors.fromcontractonstatic;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C extends Contract {
	public static @FromContract void m() {}
}