package takamaka.instrumentation.internal.checks.onClass;

import takamaka.instrumentation.internal.VerifiedClassGen;
import takamaka.instrumentation.issues.IllegalPackageNameError;

/**
 * A check that class packages in Takamaka code are allowed.
 */
public class PackagesAreLegalCheck extends VerifiedClassGen.ClassVerification.Check {

	public PackagesAreLegalCheck(VerifiedClassGen.ClassVerification verification) {
		verification.super();

		if (className.startsWith("java.") || className.startsWith("javax."))
			issue(new IllegalPackageNameError(clazz));

		if (!duringInitialization && className.startsWith("takamaka.") && !className.startsWith("takamaka.tests"))
			issue(new IllegalPackageNameError(clazz));
	}
}