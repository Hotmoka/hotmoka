package io.hotmoka.tests.cycle;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

public class Cycle extends Storage {
	private final Cycle self;
	
	public Cycle() {
		this.self = this;
	}

	public @View int foo() {
		return self.self.self.self.goo();
	}

	private int goo() {
		return 42;
	}
}