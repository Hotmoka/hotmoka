package io.takamaka.tests.basic;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

public class Simple extends Storage {
	private int i;

	public Simple(int i) {
		this.i = i;
	}

	// this is not a legal @View
	public @View void foo1() {
		i++;
	}

	// this is not a legal @View
	public @View Simple foo2() {
		return new Simple(i);
	}

	public @View int foo3() {
		return i;
	}

	public @View int foo4() {
		Simple s = new Simple(i);
		s.i++;

		return i;
	}

	public @View static int foo5() {
		Simple s = new Simple(13);
		s.i++;

		return s.i;
	}
}