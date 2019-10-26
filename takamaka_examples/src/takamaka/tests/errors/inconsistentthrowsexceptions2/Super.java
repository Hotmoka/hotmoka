package takamaka.tests.errors.inconsistentthrowsexceptions2;

import io.takamaka.lang.Contract;
import takamaka.lang.ThrowsExceptions;

public class Super extends Contract {
	public @ThrowsExceptions void m() {}
}