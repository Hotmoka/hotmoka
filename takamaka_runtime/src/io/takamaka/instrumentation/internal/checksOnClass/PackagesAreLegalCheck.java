package io.takamaka.instrumentation.internal.checksOnClass;

import io.takamaka.instrumentation.internal.VerifiedClass;
import io.takamaka.instrumentation.issues.IllegalPackageNameError;

/**
 * A check that class packages in Takamaka code are allowed.
 */
public class PackagesAreLegalCheck extends VerifiedClass.ClassVerification.Check {

	public PackagesAreLegalCheck(VerifiedClass.ClassVerification verification) {
		verification.super();

		if (className.startsWith("java.") || className.startsWith("javax."))
			issue(new IllegalPackageNameError(clazz));

		if (!duringInitialization && className.startsWith("takamaka.") && !className.startsWith("takamaka.tests"))
			issue(new IllegalPackageNameError(clazz));
	}
}