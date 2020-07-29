package io.takamaka.tests.enums;

import io.takamaka.code.lang.Storage;

public class TestEnums extends Storage {
	private final MyEnum e;

	public TestEnums(MyEnum e) {
		this.e = e;
	}

	public int getOrdinal() {
		return e.ordinal();
	}

	public static MyEnum getFor(int ordinal) {
		return MyEnum.values()[ordinal];
	}
}