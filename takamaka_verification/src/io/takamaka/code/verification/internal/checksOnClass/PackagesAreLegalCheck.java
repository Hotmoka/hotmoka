package io.takamaka.code.verification.internal.checksOnClass;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalPackageNameError;

/**
 * A check that class packages in Takamaka code are allowed.
 */
public class PackagesAreLegalCheck extends VerifiedClassImpl.ClassVerification.Check {

	public PackagesAreLegalCheck(VerifiedClassImpl.ClassVerification verification) {
		verification.super();

		if (className.startsWith("java.") || className.startsWith("javax."))
			issue(new IllegalPackageNameError(inferSourceFile()));

		if (!duringInitialization && className.startsWith("takamaka.") && !className.startsWith("takamaka.tests"))
			issue(new IllegalPackageNameError(inferSourceFile()));
	}
}