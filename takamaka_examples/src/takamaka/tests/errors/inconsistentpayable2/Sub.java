package takamaka.tests.errors.inconsistentpayable2;

import io.takamaka.code.annotations.Entry;
import io.takamaka.code.annotations.Payable;

public class Sub extends Super {
	public @Payable @Entry void m() {}
}