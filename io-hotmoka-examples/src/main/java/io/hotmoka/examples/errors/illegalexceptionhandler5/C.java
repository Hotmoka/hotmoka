package io.hotmoka.examples.errors.illegalexceptionhandler5;

public class C {
	public void m() throws Throwable {
		try {
			test();
		}
		catch (Error e) {}
	}

	private void test() throws Throwable {}
}