package io.takamaka.tests.cycle;

import io.takamaka.code.lang.Storage;

public class Cycle extends Storage {
	private final Cycle self;
	
	public Cycle() {
		this.self = this;
	}

	public int foo() {
		return self.self.self.self.goo();
	}

	private int goo() {
		return 42;
	}
}