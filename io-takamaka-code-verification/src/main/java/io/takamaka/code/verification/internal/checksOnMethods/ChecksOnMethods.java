package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;

/**
 * The manager of the set of checks on Java methods that must be applied for each version of the verification module.
 */
public interface ChecksOnMethods {

	/**
	 * Applies the checks on Java methods that must for the given version of the
	 * verification module, in their order.
	 * 
	 * @param verificationVersion the version of the verification module whose checks are queried
	 * @param context the context of the checks
	 */
	static void applyAll(int verificationVersion, VerifiedClassImpl.Builder.MethodVerification context) {
		switch (verificationVersion) {

		case 0:
			new PayableCodeReceivesAmountCheck(context);
			new RedPayableCodeReceivesAmountCheck(context);
			new ThrowsExceptionsCodeIsPublicCheck(context);
			new PayableCodeIsFromContractCheck(context);
			new RedPayableCodeIsFromContractOfRedGreenContractCheck(context);
			new EntryCodeIsInstanceAndInStorageClassCheck(context);
			new FromContractCodeIsConsistentWithClassHierarchyCheck(context);
			new PayableCodeIsConsistentWithClassHierarchyCheck(context);
			new RedPayableCodeIsConsistentWithClassHierarchyCheck(context);
			new PayableCodeIsNotRedPayableCheck(context);
			new ThrowsExceptionsIsConsistentWithClassHierarchyCheck(context);
			new IsNotStaticInitializerCheck(context);
			new IsNotNativeCheck(context);
			new IsNotFinalizerCheck(context);
			new BytecodesAreLegalCheck(context);
			new IsNotSynchronizedCheck(context);
			new CallerIsUsedOnThisAndInFromContractCheck(context);
			new ExceptionHandlersAreForCheckedExceptionsCheck(context);
			new UsedCodeIsWhiteListedCheck(context);
			new SelfChargedCodeIsInstancePublicMethodOfContractCheck(context);
			break;

		default:
			throw new IllegalArgumentException("the verification module does not support version " + verificationVersion);
		}
	}
}