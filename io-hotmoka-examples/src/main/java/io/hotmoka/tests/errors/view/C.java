package io.hotmoka.tests.errors.view;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

public class C extends Storage {
	public int x;

	public int no1(int a, int b) {
		return a + b;
	}

	public @View int yes(int a, int b) {
		return a + b;
	}

	public @View int no2(int a, int b) {
		x++;
		return a + b;
	}
}