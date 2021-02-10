package io.hotmoka.examples.errors.illegalexceptionhandler4;

public class C {
	public void m() throws Throwable {
		try {
			test();
		}
		catch (RuntimeException e) {}
	}

	private void test() throws Throwable {}
}