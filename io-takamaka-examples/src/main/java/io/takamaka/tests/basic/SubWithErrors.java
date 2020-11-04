package io.takamaka.tests.basic;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class SubWithErrors extends Super {

	public @FromContract @Payable SubWithErrors(int amount) {
		super(amount > 10 ? 13 : 17); // ok
	}

	public @FromContract SubWithErrors(boolean b) {
		super(b);
	}

	public @FromContract SubWithErrors() {
		super(true);
	}

	@Override @FromContract
	public void m1() {
		super.m1();
	}

	@Override
	public void m2() {
		super.m2();
	}

	@Override @FromContract
	public void m3() {
	}

	@Override @Payable @FromContract
	public String m4(int amount) {
		return "Sub.m4 receives " + amount + " coins from " + caller();
	}

	@Override @Payable @FromContract
	public String m4_1(long amount) {
		return "Sub.m4_1 receives " + amount + " coins from " + caller();
	}

	@Override @Payable @FromContract
	public String m4_2(BigInteger amount) {
		return "Sub.m4_2 receives " + amount + " coins from " + caller();
	}

	public void m5() {
		super.m2(); // ok
	}
}