package io.takamaka.tests.errors.legalstaticinitialization3;

public enum MyEnum {
	FIRST, SECOND;

	public final static double d = 3.1415; // legal
}