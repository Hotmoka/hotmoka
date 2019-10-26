package takamaka.tests.errors.payablewithoutentry1;

import io.takamaka.code.annotations.Payable;
import io.takamaka.lang.Contract;

public class PayableWithoutEntry extends Contract {
	public @Payable void m(int amount) {};
}