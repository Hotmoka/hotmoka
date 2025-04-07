/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.node.api;

import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.values.StorageReference;

/**
 * An exception thrown when a field of a storage object has been assigned
 * to a value that is not allowed in storage. Most of these situations are already
 * excluded by static analysis (see the verifier
 * {@code io.hotmoka.verification.internal.checksOnClass.StorageClassesHaveFieldsOfStorageTypeCheck}.
 * But Hotmoka still allows fields of declared type {@code Object} or interface,
 * to cope with generic types (often erased into {@code Object} and with fields of
 * interface type. These situations must be checked dynamically and, if violated,
 * throw this exception.
 */
@SuppressWarnings("serial")
public class IllegalAssignmentToFieldInStorage extends HotmokaException {

	/**
	 * Creates an exception about an illegal assignment of a value to a field.
	 * 
	 * @param field the field
	 * @param reference the reference of the object whose field is assigned
	 * @param assignedValue to value assigned to {@code field} of {@code reference}
	 */
	public IllegalAssignmentToFieldInStorage(FieldSignature field, StorageReference reference, Object assignedValue) {
		super("Field " + field + " of " + reference + " cannot hold a " + assignedValue.getClass().getName());
	}
}