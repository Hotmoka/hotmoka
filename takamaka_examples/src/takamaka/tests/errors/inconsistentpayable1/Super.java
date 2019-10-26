package takamaka.tests.errors.inconsistentpayable1;

import io.takamaka.code.annotations.Entry;
import io.takamaka.code.annotations.Payable;
import io.takamaka.code.lang.Contract;

public class Super extends Contract {
	public @Payable @Entry void m() {}
}