package io.takamaka.tests.basic;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.tests.basicdependency.InternationalTime;
import io.takamaka.tests.basicdependency.Time;

public abstract class Super extends Contract {
	private String s;
	@SuppressWarnings("unused")
	private InternationalTime t;

	protected Super(int a) {}
	public @FromContract Super(boolean b) {}
	public @FromContract @Payable Super(int amount, int a, boolean b) {}
	public @FromContract void m1() {}
	public void m2() {}
	public abstract @FromContract void m3();
	public abstract @FromContract @Payable String m4(int amount);
	public abstract @FromContract @Payable String m4_1(long amount);
	public abstract @FromContract @Payable String m4_2(BigInteger amount);
	public @FromContract void print(Time time) {
		s = time.toString();
	}

	public @FromContract void storeItalian(Time time) {
		if (time instanceof InternationalTime)
			t = (InternationalTime) time;
	}

	@Override
	public String toString() {
		return s;
	}
}