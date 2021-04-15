package io.takamaka.code.verification.internal.checksOnClass;

import io.takamaka.code.verification.internal.CheckOnClasses;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalBootstrapMethodError;

import java.util.Optional;

/**
 * A check that lambda bootstraps are only among those allowed by Takamaka.
 */
public class BootstrapsAreLegalCheck extends CheckOnClasses {

	public BootstrapsAreLegalCheck(VerifiedClassImpl.Verification builder) {
		super(builder);

		bootstraps.getBootstraps()
			.map(bootstraps::getTargetOf)
			.filter(Optional::isEmpty)
			.findAny()
			.ifPresent(target -> issue(new IllegalBootstrapMethodError(inferSourceFile())));
	}
}