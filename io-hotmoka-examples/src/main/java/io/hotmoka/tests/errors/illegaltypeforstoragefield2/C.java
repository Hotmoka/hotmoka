package io.hotmoka.tests.errors.illegaltypeforstoragefield2;

import io.takamaka.code.lang.Storage;

public class C extends Storage {
	// the content of this field must be checked at run time, to verify that it is a storage value
	private Object f;

	public C(Object o) {
		this.f = o;
	}

	@Override
	public String toString() {
		return String.valueOf(f);
	}
}