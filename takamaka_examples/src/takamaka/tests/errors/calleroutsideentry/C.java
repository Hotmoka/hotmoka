package takamaka.tests.errors.calleroutsideentry;

import takamaka.lang.Contract;

public class C extends Contract {
	public void m() {
		caller();
	}
}