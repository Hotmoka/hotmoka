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

		// io.takamaka.code.* is allowed during blockchain initialization, in order to
		// allow the installation of the run-time Takamaka classes such as Contract
		if (!duringInitialization && className.startsWith("io.takamaka.code."))
			issue(new IllegalPackageNameError(inferSourceFile()));
	}
}