package io.takamaka.tests.errors.illegalcalltoentry7;

import java.util.function.Supplier;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

@Exported
public class C extends Storage {

	// illegal because this class is not a contract
	public @Entry Contract entry3() {
		Supplier<Contract> target = this::getCaller;
		return target.get();
	}

	public @Entry Contract getCaller() {
		return caller();
	}
}