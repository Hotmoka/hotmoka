/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.beans.api.updates;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.beans.api.values.StorageValue;

/**
 * Implementation of an update of a field of an object.
 */
@Immutable
public interface UpdateOfField extends Update {

	/**
	 * Yields the field whose value is updated.
	 *
	 * @return the field
	 */
	FieldSignature getField();

	/**
	 * Yields the value set into the updated field.
	 * 
	 * @return the value
	 */
	StorageValue getValue();
}