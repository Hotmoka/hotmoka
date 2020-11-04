package io.takamaka.tests.errors.calleroutsidefromcontract2;

import java.util.stream.IntStream;

import io.takamaka.code.lang.Contract;

public class C extends Contract {
	public void m() {
		IntStream.iterate(0, i -> i < 10, i -> i + 1).mapToObj(i -> this.caller()).count();
	}
}