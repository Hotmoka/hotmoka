package takamaka.tests.errors.illegalsynchronized2;

public class C {
	private final Object o = new Object();
	@SuppressWarnings("unused")
	private int i;

	public void foo() {
		synchronized(o) {
			i++;
		}
	}
}