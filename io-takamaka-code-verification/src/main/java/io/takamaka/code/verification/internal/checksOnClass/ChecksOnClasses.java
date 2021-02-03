package io.takamaka.code.verification.internal.checksOnClass;

import io.takamaka.code.verification.internal.VerifiedClassImpl;

/**
 * The manager of the set of checks on Java classes that must be applied for each version of the verification module.
 */
public interface ChecksOnClasses {

	/**
	 * Applies the checks on Java methods that must for the given version of the
	 * verification module, in their order.
	 * 
	 * @param verificationVersion the version of the verification module whose checks are queried
	 * @param context the context of the checks
	 */
	static void applyAll(int verificationVersion, VerifiedClassImpl.Builder context) {
		switch (verificationVersion) {

		case 0:
			new PackagesAreLegalCheck(context);
			new NamesDontStartWithForbiddenPrefix(context);
			new BootstrapsAreLegalCheck(context);
			new StorageClassesHaveFieldsOfStorageTypeCheck(context);
			new FromContractCodeIsCalledInCorrectContextCheck(context);
			new RedPayableIsOnlyCalledFromRedGreenContractsCheck(context);
			break;

		default:
			throw new IllegalArgumentException("the verification module does not support version " + verificationVersion);
		}
	}
}