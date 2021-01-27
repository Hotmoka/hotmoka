package io.hotmoka.tests.errors.illegalcalltofromcontract7;

import java.util.function.Supplier;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

@Exported
public class C extends Storage {

	// illegal because this class is not a contract
	public @FromContract Contract entry3() {
		Supplier<Contract> target = this::getCaller;
		return target.get();
	}

	public @FromContract Contract getCaller() {
		return caller();
	}
}