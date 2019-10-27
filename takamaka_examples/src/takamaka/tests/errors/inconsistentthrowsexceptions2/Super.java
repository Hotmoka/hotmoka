package takamaka.tests.errors.inconsistentthrowsexceptions2;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.ThrowsExceptions;

public class Super extends Contract {
	public @ThrowsExceptions void m() {}
}