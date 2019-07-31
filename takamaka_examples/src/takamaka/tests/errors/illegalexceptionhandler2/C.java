package takamaka.tests.errors.illegalexceptionhandler2;

public class C {
	public void m() throws Throwable {
		try {
			test();
		}
		catch (Throwable e) {}
	}

	private void test() throws Throwable {}
}