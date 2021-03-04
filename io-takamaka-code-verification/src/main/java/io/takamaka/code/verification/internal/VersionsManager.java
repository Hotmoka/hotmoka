package io.takamaka.code.verification.internal;

import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.UnsupportedVerificationVersionError;
import io.takamaka.code.verification.internal.checksOnClass.BootstrapsAreLegalCheck;
import io.takamaka.code.verification.internal.checksOnClass.FromContractCodeIsCalledInCorrectContextCheck;
import io.takamaka.code.verification.internal.checksOnClass.NamesDontStartWithForbiddenPrefix;
import io.takamaka.code.verification.internal.checksOnClass.PackagesAreLegalCheck;
import io.takamaka.code.verification.internal.checksOnClass.RedPayableIsOnlyCalledFromRedGreenContractsCheck;
import io.takamaka.code.verification.internal.checksOnClass.StorageClassesHaveFieldsOfStorageTypeCheck;
import io.takamaka.code.verification.internal.checksOnMethods.AmountIsNotModifiedInConstructorChaining;
import io.takamaka.code.verification.internal.checksOnMethods.BytecodesAreLegalCheck;
import io.takamaka.code.verification.internal.checksOnMethods.CallerIsUsedOnThisAndInFromContractCheck;
import io.takamaka.code.verification.internal.checksOnMethods.FromContractCodeIsInstanceAndInStorageClassCheck;
import io.takamaka.code.verification.internal.checksOnMethods.ExceptionHandlersAreForCheckedExceptionsCheck;
import io.takamaka.code.verification.internal.checksOnMethods.FromContractCodeIsConsistentWithClassHierarchyCheck;
import io.takamaka.code.verification.internal.checksOnMethods.IsNotFinalizerCheck;
import io.takamaka.code.verification.internal.checksOnMethods.IsNotNativeCheck;
import io.takamaka.code.verification.internal.checksOnMethods.IsNotStaticInitializerCheck;
import io.takamaka.code.verification.internal.checksOnMethods.IsNotSynchronizedCheck;
import io.takamaka.code.verification.internal.checksOnMethods.PayableCodeIsConsistentWithClassHierarchyCheck;
import io.takamaka.code.verification.internal.checksOnMethods.PayableCodeIsFromContractCheck;
import io.takamaka.code.verification.internal.checksOnMethods.PayableCodeIsNotRedPayableCheck;
import io.takamaka.code.verification.internal.checksOnMethods.PayableCodeReceivesAmountCheck;
import io.takamaka.code.verification.internal.checksOnMethods.RedPayableCodeIsConsistentWithClassHierarchyCheck;
import io.takamaka.code.verification.internal.checksOnMethods.RedPayableCodeIsFromContractOfRedGreenContractCheck;
import io.takamaka.code.verification.internal.checksOnMethods.RedPayableCodeReceivesAmountCheck;
import io.takamaka.code.verification.internal.checksOnMethods.SelfChargedCodeIsInstancePublicMethodOfContractCheck;
import io.takamaka.code.verification.internal.checksOnMethods.ThrowsExceptionsCodeIsPublicCheck;
import io.takamaka.code.verification.internal.checksOnMethods.ThrowsExceptionsIsConsistentWithClassHierarchyCheck;
import io.takamaka.code.verification.internal.checksOnMethods.UsedCodeIsWhiteListedCheck;

/**
 * The manager of the versions of the verification module. It knows which checks must be
 * applied for each version of the module, and their order.
 */
class VersionsManager {

	/**
	 * The version of the verification module.
	 */
	public final int verificationVersion;

	VersionsManager(int verificationVersion) {
		this.verificationVersion = verificationVersion;
	}

	/**
	 * Applies the checks on Java classes for the version of the verification module, in their order.
	 * 
	 * @param builder the context of the checks
	 */
	void applyAllClassChecks(VerifiedClassImpl.Verification builder) {
		switch (verificationVersion) {

		case 0:
			new PackagesAreLegalCheck(builder);
			new NamesDontStartWithForbiddenPrefix(builder);
			new BootstrapsAreLegalCheck(builder);
			new StorageClassesHaveFieldsOfStorageTypeCheck(builder);
			new FromContractCodeIsCalledInCorrectContextCheck(builder);
			new RedPayableIsOnlyCalledFromRedGreenContractsCheck(builder);
			break;

		default:
			throw new UnsupportedVerificationVersionError(verificationVersion);
		}
	}

	/**
	 * Applies the checks on Java methods that must for the version of the verification module, in their order.
	 * 
	 * @param context the context of the checks
	 * @param method the method to check
	 */
	void applyAllMethodChecks(VerifiedClassImpl.Verification context, MethodGen method) {
		switch (verificationVersion) {

		case 0:
			new PayableCodeReceivesAmountCheck(context, method);
			new RedPayableCodeReceivesAmountCheck(context, method);
			new ThrowsExceptionsCodeIsPublicCheck(context, method);
			new PayableCodeIsFromContractCheck(context, method);
			new RedPayableCodeIsFromContractOfRedGreenContractCheck(context, method);
			new FromContractCodeIsInstanceAndInStorageClassCheck(context, method);
			new FromContractCodeIsConsistentWithClassHierarchyCheck(context, method);
			new PayableCodeIsConsistentWithClassHierarchyCheck(context, method);
			new RedPayableCodeIsConsistentWithClassHierarchyCheck(context, method);
			new PayableCodeIsNotRedPayableCheck(context, method);
			new ThrowsExceptionsIsConsistentWithClassHierarchyCheck(context, method);
			new IsNotStaticInitializerCheck(context, method);
			new IsNotNativeCheck(context, method);
			new IsNotFinalizerCheck(context, method);
			new BytecodesAreLegalCheck(context, method);
			new IsNotSynchronizedCheck(context, method);
			new CallerIsUsedOnThisAndInFromContractCheck(context, method);
			new ExceptionHandlersAreForCheckedExceptionsCheck(context, method);
			new UsedCodeIsWhiteListedCheck(context, method);
			new SelfChargedCodeIsInstancePublicMethodOfContractCheck(context, method);
			new AmountIsNotModifiedInConstructorChaining(context, method);
			break;

		default:
			throw new UnsupportedVerificationVersionError(verificationVersion);
		}
	}
}