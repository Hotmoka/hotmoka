package takamaka.tests.errors.payablewithoutentry2;

import io.takamaka.code.annotations.Payable;

public interface PayableWithoutEntry {
	public @Payable void m(int amount);
}