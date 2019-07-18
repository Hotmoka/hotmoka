package takamaka.tests.errors.illegalstaticinitialization4;

import takamaka.lang.Takamaka;

public enum MyEnum {
	FIRST, SECOND;

	public final static double d = 3.1415 * Takamaka.now(); // illegal with or without final
}