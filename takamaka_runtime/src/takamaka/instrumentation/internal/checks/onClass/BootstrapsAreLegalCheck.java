package takamaka.instrumentation.internal.checks.onClass;

import takamaka.instrumentation.internal.ClassBootstraps;
import takamaka.instrumentation.internal.VerifiedClassGen;
import takamaka.instrumentation.issues.IllegalBootstrapMethodError;

/**
 * A check that lambda bootstraps are only among those allowed by Takamaka.
 */
public class BootstrapsAreLegalCheck extends VerifiedClassGen.ClassVerification.Check {

	public BootstrapsAreLegalCheck(VerifiedClassGen.ClassVerification verification) {
		verification.super();

		ClassBootstraps bootstraps = clazz.getClassBootstraps();
		bootstraps.getBootstraps()
			.map(bootstraps::getTargetOf)
			.filter(target -> !target.isPresent())
			.findAny()
			.ifPresent(target -> issue(new IllegalBootstrapMethodError(clazz)));
	}
}