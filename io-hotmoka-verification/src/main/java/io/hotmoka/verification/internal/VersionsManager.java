/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.verification.internal;

import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.internal.checksOnClass.BootstrapsAreLegalCheck;
import io.hotmoka.verification.internal.checksOnClass.FromContractCodeIsCalledInCorrectContextCheck;
import io.hotmoka.verification.internal.checksOnClass.NamesDontStartWithForbiddenPrefix;
import io.hotmoka.verification.internal.checksOnClass.PackagesAreLegalCheck;
import io.hotmoka.verification.internal.checksOnClass.StorageClassesHaveFieldsOfStorageTypeCheck;
import io.hotmoka.verification.internal.checksOnMethods.AmountIsNotModifiedInConstructorChaining;
import io.hotmoka.verification.internal.checksOnMethods.BytecodesAreLegalCheck;
import io.hotmoka.verification.internal.checksOnMethods.CallerIsUsedOnThisAndInFromContractCheck;
import io.hotmoka.verification.internal.checksOnMethods.ExceptionHandlersAreForCheckedExceptionsCheck;
import io.hotmoka.verification.internal.checksOnMethods.FromContractCodeIsConsistentWithClassHierarchyCheck;
import io.hotmoka.verification.internal.checksOnMethods.FromContractCodeIsInstanceAndInStorageClassCheck;
import io.hotmoka.verification.internal.checksOnMethods.IsNotFinalizerCheck;
import io.hotmoka.verification.internal.checksOnMethods.IsNotNativeCheck;
import io.hotmoka.verification.internal.checksOnMethods.IsNotStaticInitializerCheck;
import io.hotmoka.verification.internal.checksOnMethods.IsNotSynchronizedCheck;
import io.hotmoka.verification.internal.checksOnMethods.PayableCodeIsConsistentWithClassHierarchyCheck;
import io.hotmoka.verification.internal.checksOnMethods.PayableCodeIsFromContractCheck;
import io.hotmoka.verification.internal.checksOnMethods.PayableCodeReceivesAmountCheck;
import io.hotmoka.verification.internal.checksOnMethods.ThrowsExceptionsCodeIsPublicCheck;
import io.hotmoka.verification.internal.checksOnMethods.ThrowsExceptionsIsConsistentWithClassHierarchyCheck;
import io.hotmoka.verification.internal.checksOnMethods.UsedCodeIsWhiteListedCheck;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

/**
 * The manager of the versions of the verification module. It knows which checks must be
 * applied for each version of the module, and their order.
 */
final class VersionsManager {

	/**
	 * The version of the verification module.
	 */
	public final long verificationVersion;

	VersionsManager(long verificationVersion) throws UnsupportedVerificationVersionException {
		if (verificationVersion != 0L)
			throw new UnsupportedVerificationVersionException(verificationVersion);

		this.verificationVersion = verificationVersion;
	}

	/**
	 * Applies the checks on Java classes for the version of the verification module, in their order.
	 * 
	 * @param builder the context of the checks
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	void applyAllClassChecks(VerifiedClassImpl.Verification builder) throws ClassNotFoundException {
		if (verificationVersion == 0L) {
			new PackagesAreLegalCheck(builder);
			new NamesDontStartWithForbiddenPrefix(builder);
			new BootstrapsAreLegalCheck(builder);
			new StorageClassesHaveFieldsOfStorageTypeCheck(builder);
			new FromContractCodeIsCalledInCorrectContextCheck(builder);
		}
	}

	/**
	 * Applies the checks on Java methods that must for the version of the verification module, in their order.
	 * 
	 * @param context the context of the checks
	 * @param method the method to check
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	void applyAllMethodChecks(VerifiedClassImpl.Verification context, MethodGen method) throws ClassNotFoundException {
		if (verificationVersion == 0L) {
			new PayableCodeReceivesAmountCheck(context, method);
			new ThrowsExceptionsCodeIsPublicCheck(context, method);
			new PayableCodeIsFromContractCheck(context, method);
			new FromContractCodeIsInstanceAndInStorageClassCheck(context, method);
			new FromContractCodeIsConsistentWithClassHierarchyCheck(context, method);
			new PayableCodeIsConsistentWithClassHierarchyCheck(context, method);
			new ThrowsExceptionsIsConsistentWithClassHierarchyCheck(context, method);
			new IsNotStaticInitializerCheck(context, method);
			new IsNotNativeCheck(context, method);
			new IsNotFinalizerCheck(context, method);
			new BytecodesAreLegalCheck(context, method);
			new IsNotSynchronizedCheck(context, method);
			new CallerIsUsedOnThisAndInFromContractCheck(context, method);
			new ExceptionHandlersAreForCheckedExceptionsCheck(context, method);
			new UsedCodeIsWhiteListedCheck(context, method);
			new AmountIsNotModifiedInConstructorChaining(context, method);
		}
	}
}