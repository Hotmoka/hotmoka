package takamaka.verifier;

import org.apache.bcel.generic.ClassGen;

public class IllegalCallSiteResolverError extends Error {

	public IllegalCallSiteResolverError(ClassGen clazz, String resolverClassName, String resolverMethodName) {
		super(clazz, "Illegal call site resolver " + resolverClassName + "." + resolverMethodName);
	}
}