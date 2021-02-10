package io.hotmoka.examples.errors.finalize;

public class C {

	@Override
	protected void finalize() {} // illegal in Takamaka
}