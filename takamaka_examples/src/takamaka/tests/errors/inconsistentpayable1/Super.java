package takamaka.tests.errors.inconsistentpayable1;

import io.takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;

public class Super extends Contract {
	public @Payable @Entry void m() {}
}