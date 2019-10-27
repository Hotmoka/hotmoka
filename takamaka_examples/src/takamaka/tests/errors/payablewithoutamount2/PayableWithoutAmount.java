package takamaka.tests.errors.payablewithoutamount2;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Payable;

public class PayableWithoutAmount extends Contract {
	public @Payable @Entry void m(float amount) {};
}