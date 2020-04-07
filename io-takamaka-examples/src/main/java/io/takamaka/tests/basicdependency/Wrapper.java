package io.takamaka.tests.basicdependency;

import java.math.BigInteger;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

public class Wrapper extends Storage {
	private final Time time;
	private final String s;
	private final BigInteger bi;
	private final long l;

	public Wrapper(Time time) {
		this(time, null, null, 0L);
	}

	public Wrapper(Time time, String s, BigInteger bi, long l) {
		this.time = time;
		this.s = s;
		this.bi = bi;
		this.l = l;
	}

	@Override @View
	public String toString() {
		return "wrapper(" + time + "," + s + "," + bi + "," + l + ")";
	}
}