package io.hotmoka.tests.errors.illegaltypeforstoragefield1;

import io.takamaka.code.lang.Storage;

public class C extends Storage {
	public final NonStorage s;

	public C(NonStorage s) {
		this.s = s;
	}
}