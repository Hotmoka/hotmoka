package takamaka.tests.errors.inconsistentpayable2;

import takamaka.lang.Entry;
import takamaka.lang.Payable;

public class Sub extends Super {
	public @Payable @Entry void m() {}
}