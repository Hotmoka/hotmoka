package takamaka.tests.errors.payablewithoutentry1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Payable;

public class PayableWithoutEntry extends Contract {
	public @Payable void m(int amount) {};
}