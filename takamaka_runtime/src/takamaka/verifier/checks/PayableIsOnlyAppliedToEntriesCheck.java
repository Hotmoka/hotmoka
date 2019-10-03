package takamaka.verifier.checks;

import java.util.stream.Stream;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.PayableWithoutEntryError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class PayableIsOnlyAppliedToEntriesCheck extends VerifiedClassGen.ClassVerification.ClassLevelCheck {

	public PayableIsOnlyAppliedToEntriesCheck(VerifiedClassGen.ClassVerification verification) {
		verification.super();

		Stream.of(clazz.getMethods())
			.filter(method -> classLoader.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType())
					&& classLoader.isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType()) == null)
			.forEach(method -> issue(new PayableWithoutEntryError(clazz, method)));
	}
}