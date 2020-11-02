package io.takamaka.tests.errors.illegalcalltoentry6;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

@Exported
public class C extends Storage {

	// illegal because this class is not a contract
	public @Entry Contract entry2() {
		C c = this;
		return c.getCaller();
	}

	public @Entry Contract getCaller() {
		return caller();
	}
}