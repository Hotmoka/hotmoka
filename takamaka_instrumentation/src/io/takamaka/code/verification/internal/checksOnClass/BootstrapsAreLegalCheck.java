package io.takamaka.code.verification.internal.checksOnClass;

import io.takamaka.code.verification.Bootstraps;
import io.takamaka.code.verification.VerifiedClass;
import io.takamaka.code.verification.issues.IllegalBootstrapMethodError;

/**
 * A check that lambda bootstraps are only among those allowed by Takamaka.
 */
public class BootstrapsAreLegalCheck extends VerifiedClass.ClassVerification.Check {

	public BootstrapsAreLegalCheck(VerifiedClass.ClassVerification verification) {
		verification.super();

		Bootstraps bootstraps = clazz.bootstraps;
		bootstraps.getBootstraps()
			.map(bootstraps::getTargetOf)
			.filter(target -> !target.isPresent())
			.findAny()
			.ifPresent(target -> issue(new IllegalBootstrapMethodError(inferSourceFile())));
	}
}