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

package io.hotmoka.beans.internal.values;

import java.io.IOException;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.values.EnumValue;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * An element of an enumeration stored in blockchain.
 */
@Immutable
public final class EnumValueImpl extends AbstractStorageValue implements EnumValue {
	static final byte SELECTOR = 12;

	/**
	 * The name of the class of the enumeration.
	 */
	private final String enumClassName;

	/**
	 * The name of the enumeration element.
	 */
	private final String name;

	/**
	 * Builds an element of an enumeration.
	 * 
	 * @param enumClassName the class of the enumeration
	 * @param name the name of the enumeration element
	 */
	public EnumValueImpl(String enumClassName, String name) {
		this.enumClassName = Objects.requireNonNull(enumClassName, "enumClassName cannot be null");
		this.name = Objects.requireNonNull(name, "name cannot be null");
	}

	@Override
	public String getEnumClassName() {
		return enumClassName;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return String.format("%s.%s", enumClassName, name);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EnumValue ev && ev.getName().equals(name) && ev.getEnumClassName().equals(enumClassName);
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ enumClassName.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		if (other instanceof EnumValue ev) {
			int diff = enumClassName.compareTo(ev.getEnumClassName());
			if (diff != 0)
				return diff;
			else
				return name.compareTo(ev.getName());			
		}
		else
			return super.compareTo(other);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeStringUnshared(enumClassName);
		context.writeStringUnshared(name);
	}
}