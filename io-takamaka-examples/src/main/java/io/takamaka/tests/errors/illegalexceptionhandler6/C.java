package io.takamaka.tests.errors.illegalexceptionhandler6;

public class C {
	public void m() throws Throwable {
		try {
			test();
		}
		catch (MyUncheckedException e) {}
	}

	private void test() throws Throwable {}
}