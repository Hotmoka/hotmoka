package takamaka.verifier.internal.checks.onClass;

import takamaka.verifier.errors.IllegalPackageNameError;
import takamaka.verifier.internal.VerifiedClassGenImpl;

/**
 * A check that class packages in Takamaka code are allowed.
 */
public class PackagesAreLegalCheck extends VerifiedClassGenImpl.Verification.Check {

	public PackagesAreLegalCheck(VerifiedClassGenImpl.Verification verification) {
		verification.super();

		if (className.startsWith("java.") || className.startsWith("javax."))
			issue(new IllegalPackageNameError(clazz));

		if (!duringInitialization && className.startsWith("takamaka.") && !className.startsWith("takamaka.tests"))
			issue(new IllegalPackageNameError(clazz));
	}
}