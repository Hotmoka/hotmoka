package io.takamaka.tests.errors.legalexceptionhandler1;

public class C {
	public void m() throws Throwable {
		try {
			test();
		}
		catch (MyCheckedException e) {}
	}

	private void test() throws Throwable {}
}