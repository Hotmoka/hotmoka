package takamaka.tests.errors.illegaltypeforstoragefield1;

import takamaka.lang.Storage;

public class C extends Storage {
	public final NonStorage s;

	public C(NonStorage s) {
		this.s = s;
	}
}