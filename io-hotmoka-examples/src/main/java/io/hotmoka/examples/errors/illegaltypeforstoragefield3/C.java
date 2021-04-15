package io.hotmoka.examples.errors.illegaltypeforstoragefield3;

import io.takamaka.code.lang.Storage;

public class C extends Storage {
	// the content of this field can be checked at compile time, to check if it is a storage value
	private final MyEnum f;

	public C(MyEnum o) {
		this.f = o;
	}

	@Override
	public String toString() {
		return String.valueOf(f);
	}
}