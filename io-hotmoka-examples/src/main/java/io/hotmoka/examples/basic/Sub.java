package io.hotmoka.examples.basic;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.View;

public class Sub extends Super {

	public Sub() {
		super(13);
	}

	public @FromContract @Payable Sub(int amount) {
		super(amount > 10 ? 13 : 17); // ok
	}

	@Override @FromContract @View
	public void m1() {
		super.m1();
	}

	@Override @FromContract @View
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

	@View
	public void m5() {
		super.m2(); // ok
		new Sub(13);
	}

	@View
	public static void ms() {}
}