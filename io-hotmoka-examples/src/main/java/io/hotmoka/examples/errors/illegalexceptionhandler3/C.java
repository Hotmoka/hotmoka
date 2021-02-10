package io.hotmoka.examples.errors.illegalexceptionhandler3;

public class C {
	public void m() throws Throwable {
		try {
			test();
		}
		catch (Exception e) {}
	}

	private void test() throws Throwable {}
}