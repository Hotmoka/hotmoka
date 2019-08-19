package takamaka.tests.errors.illegalcalltononwhitelistedconstructor2;

import java.util.Random;
import java.util.function.Supplier;

public class C {

	public int foo() {
		return test(Random::new); //// KO: this goes inside a bootstrap method
	}

	private int test(Supplier<Random> supplier) {
		return supplier.get().nextInt();
	}
}