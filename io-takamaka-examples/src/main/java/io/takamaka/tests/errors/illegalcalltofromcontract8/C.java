package io.takamaka.tests.errors.illegalcalltofromcontract8;

import java.util.function.Supplier;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

@Exported
public class C extends Storage {

	// illegal because this class is not a contract
	public @FromContract Contract entry4() {
		C c = this;
		Supplier<Contract> target = c::getCaller;
		return target.get();
	}

	public @FromContract Contract getCaller() {
		return caller();
	}
}