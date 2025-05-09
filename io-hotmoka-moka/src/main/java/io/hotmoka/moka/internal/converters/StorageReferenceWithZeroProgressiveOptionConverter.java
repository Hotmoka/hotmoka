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

package io.hotmoka.moka.internal.converters;

import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.values.StorageReference;
import picocli.CommandLine.ITypeConverter;

/**
 * A converter of an option into a storage reference for an account with zero progressive.
 * This converter guarantees that the account can be represented in BIP39 format.
 */
public class StorageReferenceWithZeroProgressiveOptionConverter implements ITypeConverter<StorageReference> {

	@Override
	public StorageReference convert(String value) throws IllegalArgumentException {
		var reference = StorageValues.reference(value);
		if (reference.getProgressive().signum() != 0)
			throw new IllegalArgumentException("Accounts are limited to have 0 as progressive index");

		return reference;
	}
}