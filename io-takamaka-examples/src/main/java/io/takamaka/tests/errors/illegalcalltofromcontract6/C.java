package io.takamaka.tests.errors.illegalcalltofromcontract6;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

@Exported
public class C extends Storage {

	// illegal because this class is not a contract
	public @FromContract Contract entry2() {
		C c = this;
		return c.getCaller();
	}

	public @FromContract Contract getCaller() {
		return caller();
	}
}