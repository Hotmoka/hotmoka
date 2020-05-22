package io.takamaka.tests.errors.finalize;

public class C {

	@Override
	protected void finalize() {} // illegal in Takamaka
}