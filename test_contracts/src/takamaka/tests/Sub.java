package takamaka.tests;

import takamaka.lang.Entry;
import takamaka.lang.Payable;

public class Sub extends Super {

	public Sub() {
		super(13);
	}

	public @Entry @Payable Sub(int amount) {
		super(amount > 10 ? 13 : 17); // ok
	}

	@Override @Entry
	public void m1() { // this is implicitly @Entry by inheritance
		super.m1(); // exception at run time
		System.out.println("Sub.m1");
	}

	@Override
	public void m3() { // this is implicitly @Entry
		System.out.println("Sub.m3 with caller " + caller());
	}

	@Override
	public void m4(int amount) { // this is implicitly @Payable @Entry
		System.out.println("Sub.m4 receives " + amount + " coins from " + caller());
	}

	public void m5() {
		super.m2(); // ok
		new Sub(13);
	}

	public static void ms() {}
}