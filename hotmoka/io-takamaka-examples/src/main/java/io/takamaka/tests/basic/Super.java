package io.takamaka.tests.basic;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Payable;
import io.takamaka.tests.basicdependency.InternationalTime;
import io.takamaka.tests.basicdependency.Time;

public abstract class Super extends Contract {
	private String s;
	@SuppressWarnings("unused")
	private InternationalTime t;

	protected Super(int a) {}
	public @Entry Super(boolean b) {}
	public @Entry @Payable Super(int amount, int a, boolean b) {}
	public @Entry void m1() {}
	public void m2() {}
	public abstract @Entry void m3();
	public abstract @Entry @Payable String m4(int amount);
	public abstract @Entry @Payable String m4_1(long amount);
	public abstract @Entry @Payable String m4_2(BigInteger amount);
	public @Entry void print(Time time) {
		s = time.toString();
	}

	public @Entry void storeItalian(Time time) {
		if (time instanceof InternationalTime)
			t = (InternationalTime) time;
	}

	@Override
	public String toString() {
		return s;
	}
}