package takamaka.tests.errors.illegalcalltoentry2;

public class Caller {

	public void m() {
		new C().entry(); // KO
	}
}