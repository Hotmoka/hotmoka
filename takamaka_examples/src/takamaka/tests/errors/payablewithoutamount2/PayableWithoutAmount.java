package takamaka.tests.errors.payablewithoutamount2;

import io.takamaka.code.annotations.Entry;
import io.takamaka.code.annotations.Payable;
import io.takamaka.code.lang.Contract;

public class PayableWithoutAmount extends Contract {
	public @Payable @Entry void m(float amount) {};
}