package takamaka.tests.errors.calleroutsideentry;

import io.takamaka.lang.Contract;

public class C extends Contract {
	public void m() {
		caller();
	}
}