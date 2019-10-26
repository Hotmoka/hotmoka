package takamaka.tests.errors.throwsexceptionsonnonpublic1;

import io.takamaka.lang.Contract;
import takamaka.lang.ThrowsExceptions;

public class C extends Contract {
	@ThrowsExceptions void m() {};
}