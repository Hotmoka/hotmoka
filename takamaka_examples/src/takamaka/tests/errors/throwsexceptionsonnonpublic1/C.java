package takamaka.tests.errors.throwsexceptionsonnonpublic1;

import io.takamaka.code.annotations.ThrowsExceptions;
import io.takamaka.lang.Contract;

public class C extends Contract {
	@ThrowsExceptions void m() {};
}