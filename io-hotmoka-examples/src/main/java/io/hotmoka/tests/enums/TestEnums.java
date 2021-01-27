package io.hotmoka.tests.enums;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

public class TestEnums extends Storage {
	private final MyEnum e;

	public TestEnums(MyEnum e) {
		this.e = e;
	}

	public @View int getOrdinal() {
		return e.ordinal();
	}

	public static @View MyEnum getFor(int ordinal) {
		return MyEnum.values()[ordinal];
	}
}