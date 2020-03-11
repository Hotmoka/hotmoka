package io.takamaka.tests.basic;

import java.math.BigInteger;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Payable;

public class SubWithErrors extends Super {

	public @Entry @Payable SubWithErrors(int amount) {
		super(amount > 10 ? 13 : 17); // ok
	}

	public SubWithErrors(boolean b) {
		super(b); // exception at run time
	}

	public @Entry SubWithErrors() {
		super(true); // exception at run time
	}

	@Override @Entry
	public void m1() {
		super.m1(); // exception at run time
	}

	@Override
	public void m2() {
		super.m2();
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
	}
}