package io.hotmoka.examples.errors.throwsexceptionsonnonpublic1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.ThrowsExceptions;

public class C extends Contract {
	@ThrowsExceptions void m() {}
}