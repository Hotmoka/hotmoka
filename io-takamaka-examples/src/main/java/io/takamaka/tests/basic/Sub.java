package io.takamaka.tests.basic;

import java.math.BigInteger;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Payable;

public class Sub extends Super {

	public Sub() {
		super(13);
	}

	public @Entry @Payable Sub(int amount) {
		super(amount > 10 ? 13 : 17); // ok
	}

	@Override @Entry
	public void m1() {
		super.m1(); // exception at run time
	}

	@Override @Entry
	public void m3() {
	}

	@Override @Payable @Entry
	public String m4(int amount) {
		return "Sub.m4 receives " + amount + " coins from " + caller();
	}

	@Override @Payable @Entry
	public String m4_1(long amount) {
		return "Sub.m4_1 receives " + amount + " coins from " + caller();
	}

	@Override @Payable @Entry
	public String m4_2(BigInteger amount) {
		return "Sub.m4_2 receives " + amount + " coins from " + caller();
	}

	public void m5() {
		super.m2(); // ok
		new Sub(13);
	}

	public static void ms() {}
}