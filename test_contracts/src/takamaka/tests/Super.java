package takamaka.tests;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;

public abstract class Super extends Contract {
	protected Super(int a) {}
	protected @Entry Super(boolean b) {}
	public @Entry @Payable Super(int amount, int a, boolean b) {}
	public @Entry void m1() {}
	public void m2() {}
	public abstract @Entry void m3();
	public abstract @Entry @Payable void m4(int amount);
}