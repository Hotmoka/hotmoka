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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.verification.ThrowIncompleteClasspathError;
import io.hotmoka.verification.internal.CheckOnClasses;
import io.hotmoka.verification.internal.VerifiedClassImpl;
import io.hotmoka.verification.issues.IllegalTypeForStorageFieldError;

/**
 * A checks that payable methods have an amount first argument.
 */
public class StorageClassesHaveFieldsOfStorageTypeCheck extends CheckOnClasses {

	public StorageClassesHaveFieldsOfStorageTypeCheck(VerifiedClassImpl.Verification builder) {
		super(builder);

		if (classLoader.isStorage(className))
			ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() ->
				Stream.of(classLoader.loadClass(className).getDeclaredFields())
					.filter(field -> !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
					.filter(field -> !isTypeAllowedForStorageFields(field.getType()))
					.map(field -> new IllegalTypeForStorageFieldError(inferSourceFile(), field.getName(), field.getType().isEnum()))
					.forEach(this::issue));
	}

	@SuppressWarnings("unchecked")
	private boolean isTypeAllowedForStorageFields(Class<?> type) {
		// we allow Object since it can be the erasure of a generic type: the runtime of Takamaka
		// will check later if the actual type of the object in this field is allowed;
		// we also allow interfaces since they cannot extend Storage and only at run time it will
		// be possible to determine if the content is a storage value
		return type.isPrimitive() || type == Object.class || type.isInterface() || type == String.class || type == BigInteger.class
			|| (type.isEnum() && !hasInstanceFields((Class<? extends Enum<?>>) type))
			|| (!type.isArray() && classLoader.isStorage(type.getName()));
	}

	/**
	 * Determines if the given enumeration type has at least an instance, non-transient field.
	 * 
	 * @param clazz the class
	 * @return true only if that condition holds
	 */
	private static boolean hasInstanceFields(Class<? extends Enum<?>> clazz) {
		return Stream.of(clazz.getDeclaredFields())
			.map(Field::getModifiers)
			.anyMatch(modifiers -> !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers));
	}
}