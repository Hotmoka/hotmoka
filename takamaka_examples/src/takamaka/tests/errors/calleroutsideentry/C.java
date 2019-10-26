package takamaka.tests.errors.calleroutsideentry;

import io.takamaka.code.lang.Contract;

public class C extends Contract {
	public void m() {
		caller();
	}
}