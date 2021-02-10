package io.hotmoka.examples.errors.calleroutsidefromcontract3;

import java.util.stream.IntStream;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C extends Contract {
	public @FromContract void m() {
		IntStream.iterate(0, i -> i < 10, i -> i + 1).mapToObj(i -> this.caller()).count();
	}
}