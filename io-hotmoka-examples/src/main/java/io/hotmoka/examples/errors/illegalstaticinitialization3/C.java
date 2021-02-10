package io.hotmoka.examples.errors.illegalstaticinitialization3;

import io.takamaka.code.lang.Takamaka;

public class C {
	public final static double d = 3.1415 * Takamaka.now(); // illegal with or without final
}