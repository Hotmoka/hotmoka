package takamaka.tests;

import takamaka.lang.Storage;

public class Wrapper extends Storage {
	private final Time time;

	public Wrapper(Time time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return "wrapper of " + time.toString();
	}
}