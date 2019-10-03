package takamaka.verifier.checks;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalBootstrapMethodError;

/**
 * A check that lambda bootstraps are only among those allowed by Takamaka.
 */
public class BootstrapsAreLegalCheck extends VerifiedClassGen.ClassVerification.ClassLevelCheck {

	public BootstrapsAreLegalCheck(VerifiedClassGen.ClassVerification verification) {
		verification.super();

		classBootstraps.getBootstraps()
			.map(this::getTargetOf)
			.filter(target -> !target.isPresent())
			.findAny()
			.ifPresent(target -> issue(new IllegalBootstrapMethodError(clazz)));
	}
}