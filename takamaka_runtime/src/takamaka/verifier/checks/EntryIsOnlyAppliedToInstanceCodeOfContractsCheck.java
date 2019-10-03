package takamaka.verifier.checks;

import org.apache.bcel.classfile.Method;

import takamaka.translator.TakamakaClassLoader;
import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalEntryArgumentError;
import takamaka.verifier.errors.IllegalEntryMethodError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class EntryIsOnlyAppliedToInstanceCodeOfContractsCheck extends VerifiedClassGen.ClassVerification.ClassLevelCheck {

	public EntryIsOnlyAppliedToInstanceCodeOfContractsCheck(VerifiedClassGen.ClassVerification verification) {
		verification.super();

		TakamakaClassLoader classLoader = clazz.getClassLoader();
		boolean isContract = classLoader.isContract(className);

		for (Method method: clazz.getMethods()) {
			Class<?> isEntry = classLoader.isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType());
			if (isEntry != null) {
				if (!classLoader.contractClass.isAssignableFrom(isEntry))
					issue(new IllegalEntryArgumentError(clazz, method));
				if (method.isStatic() || !isContract)
					issue(new IllegalEntryMethodError(clazz, method));
			}
		}
	}
}