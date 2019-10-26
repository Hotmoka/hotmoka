package takamaka.tests.errors.payablewithoutamount1;

import io.takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;

public class PayableWithoutAmount extends Contract {
	public @Payable @Entry void m() {};
}