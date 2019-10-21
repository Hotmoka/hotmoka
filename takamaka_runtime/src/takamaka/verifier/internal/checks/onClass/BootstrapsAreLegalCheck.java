package takamaka.verifier.internal.checks.onClass;

import takamaka.verifier.ClassBootstraps;
import takamaka.verifier.errors.IllegalBootstrapMethodError;
import takamaka.verifier.internal.VerifiedClassGenImpl;

/**
 * A check that lambda bootstraps are only among those allowed by Takamaka.
 */
public class BootstrapsAreLegalCheck extends VerifiedClassGenImpl.Verification.Check {

	public BootstrapsAreLegalCheck(VerifiedClassGenImpl.Verification verification) {
		verification.super();

		ClassBootstraps bootstraps = clazz.getClassBootstraps();
		bootstraps.getBootstraps()
			.map(bootstraps::getTargetOf)
			.filter(target -> !target.isPresent())
			.findAny()
			.ifPresent(target -> issue(new IllegalBootstrapMethodError(clazz)));
	}
}