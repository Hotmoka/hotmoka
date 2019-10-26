package takamaka.tests.errors.inconsistentthrowsexceptions2;

import io.takamaka.code.annotations.ThrowsExceptions;
import io.takamaka.code.lang.Contract;

public class Super extends Contract {
	public @ThrowsExceptions void m() {}
}