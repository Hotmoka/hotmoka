package takamaka.verifier.checks.onMethod;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalEntryArgumentError;
import takamaka.verifier.errors.IllegalEntryMethodError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class EntryCodeIsInstanceAndInContractsCheck extends VerifiedClassGen.Verifier.MethodVerifier.Check {

	public EntryCodeIsInstanceAndInContractsCheck(VerifiedClassGen.Verifier.MethodVerifier verifier) {
		verifier.super();

		boolean isContract = classLoader.isContract(className);
		Class<?> isEntry = classLoader.isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType());

		if (isEntry != null) {
			if (!classLoader.contractClass.isAssignableFrom(isEntry))
				issue(new IllegalEntryArgumentError(clazz, method));
			if (method.isStatic() || !isContract)
				issue(new IllegalEntryMethodError(clazz, method));
		}
	}
}