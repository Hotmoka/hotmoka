package takamaka.tests.errors.payablewithoutentry1;

import io.takamaka.lang.Contract;
import takamaka.lang.Payable;

public class PayableWithoutEntry extends Contract {
	public @Payable void m(int amount) {};
}