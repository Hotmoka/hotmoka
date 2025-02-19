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

package io.hotmoka.verification.internal.checksOnClass;

import static io.hotmoka.exceptions.CheckRunnable.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.errors.IllegalTypeForStorageFieldError;
import io.hotmoka.verification.internal.CheckOnClasses;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A checks that payable methods have an amount first argument.
 */
public class StorageClassesHaveFieldsOfStorageTypeCheck extends CheckOnClasses {

	public StorageClassesHaveFieldsOfStorageTypeCheck(VerifiedClassImpl.Verification builder) throws IllegalJarException {
		super(builder);

		boolean isStorage;

		try {
			isStorage = classLoader.isStorage(className);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalJarException(e);
		}

		if (isStorage) {
			Class<?> clazz;

			try {
				clazz = classLoader.loadClass(className);
			}
			catch (ClassNotFoundException e) {
				// the class under verification is part of the jar hence it must be found by the class loader
				throw new RuntimeException(e);
			}

			check(IllegalJarException.class, () ->
				Stream.of(clazz.getDeclaredFields())
					.filter(field -> !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
					.filter(uncheck(IllegalJarException.class, field -> !isTypeAllowedForStorageFields(field.getType())))
					.map(field -> new IllegalTypeForStorageFieldError(inferSourceFile(), field.getName()))
					.forEachOrdered(this::issue)
			);
		}
	}

	private boolean isTypeAllowedForStorageFields(Class<?> type) throws IllegalJarException {
		// we allow Object since it can be the erasure of a generic type: the runtime of Takamaka
		// will check later if the actual type of the object in this field is allowed;
		// we also allow interfaces since they cannot extend Storage and only at run time it will
		// be possible to determine if the content is a storage value
		try {
			return type.isPrimitive() || type == Object.class || type.isInterface() || type == String.class || type == BigInteger.class
				|| (!type.isArray() && classLoader.isStorage(type.getName()));
		}
		catch (ClassNotFoundException e) {
			throw new IllegalJarException(e);
		}
	}
}