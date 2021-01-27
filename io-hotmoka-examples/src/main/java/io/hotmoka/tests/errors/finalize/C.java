package io.hotmoka.tests.errors.finalize;

public class C {

	@Override
	protected void finalize() {} // illegal in Takamaka
}