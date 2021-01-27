package io.hotmoka.tests.errors.illegaltypeforstoragefield4;

import io.takamaka.code.lang.Storage;

public class C extends Storage {
	// the content of this field must be checked at run time, to verify that it is a storage value
	private MyInterface f;

	public C() {
		this.f = new NonStorage(); // will fail at run time
	}

	@Override
	public String toString() {
		return String.valueOf(f);
	}
}