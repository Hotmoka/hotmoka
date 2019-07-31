package takamaka.tests.errors.illegalexceptionhandler1;

public class C {
	public void m() throws Throwable {
		try {
			test();
		}
		catch (NullPointerException e) {}
	}

	private void test() throws Throwable {}
}