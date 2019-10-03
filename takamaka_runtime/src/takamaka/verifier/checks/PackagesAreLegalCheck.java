package takamaka.verifier.checks;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalPackageNameError;

/**
 * A check that class packages in Takamaka code are allowed.
 */
public class PackagesAreLegalCheck extends VerifiedClassGen.ClassVerification.ClassLevelCheck {

	public PackagesAreLegalCheck(VerifiedClassGen.ClassVerification verification) {
		verification.super();

		if (className.startsWith("java.") || className.startsWith("javax."))
			issue(new IllegalPackageNameError(clazz));

		if (!duringInitialization && className.startsWith("takamaka.") && !className.startsWith("takamaka.tests"))
			issue(new IllegalPackageNameError(clazz));
	}
}