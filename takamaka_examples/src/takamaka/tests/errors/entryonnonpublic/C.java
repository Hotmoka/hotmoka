package takamaka.tests.errors.entryonnonpublic;

import takamaka.lang.Contract;
import takamaka.lang.Entry;

public class C extends Contract {
	protected @Entry void m() {}
}