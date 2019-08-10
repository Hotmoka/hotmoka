package takamaka.tests.errors.illegalcalltononwhitelistedconstructor1;

import java.util.Random;

public class C {

	public int foo() {
		Random random = new Random(); // KO
		return random.nextInt();
	}
}