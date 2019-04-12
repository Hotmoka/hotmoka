package takamaka.tests;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;

public abstract class Super extends Contract {
	private String s;
	@SuppressWarnings("unused")
	private ItalianTime t;

	protected Super(int a) {}
	protected @Entry Super(boolean b) {}
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
		if (time instanceof ItalianTime)
			t = (ItalianTime) time;
	}

	@Override
	public String toString() {
		return s;
	}
}