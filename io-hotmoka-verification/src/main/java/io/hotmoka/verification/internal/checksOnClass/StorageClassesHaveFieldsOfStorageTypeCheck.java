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

import java.lang.reflect.Modifier;
import java.math.BigInteger;

import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.errors.IllegalTypeForStorageFieldError;
import io.hotmoka.verification.internal.CheckOnClasses;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A checks that payable methods have an amount first argument.
 */
public class StorageClassesHaveFieldsOfStorageTypeCheck extends CheckOnClasses {

	public StorageClassesHaveFieldsOfStorageTypeCheck(VerifiedClassImpl.Verification builder) throws UnknownTypeException {
		super(builder);

		if (isStorage)
			for (var field: clazz.getDeclaredFields())
				if (!Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()) && !isTypeAllowedForStorageFields(field.getType()))
					issue(new IllegalTypeForStorageFieldError(inferSourceFile(), field.getName()));
	}

	private boolean isTypeAllowedForStorageFields(Class<?> type) throws UnknownTypeException {
		// we allow Object since it can be the erasure of a generic type: the runtime of Takamaka
		// will check later if the actual type of the object in this field is allowed
		// (see the {@code UpdatesExtractor} class);
		// we also allow interfaces since they cannot extend Storage and only at run time it will
		// be possible to determine if the content is a storage value
		try {
			return type.isPrimitive() || type == Object.class || type.isInterface() || type == String.class || type == BigInteger.class
				|| (!type.isArray() && classLoader.isStorage(type.getName())); // TODO: provide isStorage for class?
		}
		catch (ClassNotFoundException e) {
			throw new UnknownTypeException(type.getName());
		}
	}
}