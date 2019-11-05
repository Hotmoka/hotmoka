package io.takamaka.code.verification.internal.checksOnClass;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalBootstrapMethodError;

/**
 * A check that lambda bootstraps are only among those allowed by Takamaka.
 */
public class BootstrapsAreLegalCheck extends VerifiedClassImpl.ClassVerification.Check {

	public BootstrapsAreLegalCheck(VerifiedClassImpl.ClassVerification verification) {
		verification.super();

		bootstraps.getBootstraps()
			.map(bootstraps::getTargetOf)
			.filter(target -> !target.isPresent())
			.findAny()
			.ifPresent(target -> issue(new IllegalBootstrapMethodError(inferSourceFile())));
	}
}