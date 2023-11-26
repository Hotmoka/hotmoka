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

package io.hotmoka.beans.values;

import java.io.IOException;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * An element of an enumeration stored in blockchain.
 */
@Immutable
public final class EnumValue extends StorageValue {
	static final byte SELECTOR = 12;

	/**
	 * The name of the class of the enumeration.
	 */
	public final String enumClassName;

	/**
	 * The name of the enumeration element.
	 */
	public final String name;

	/**
	 * Builds an element of an enumeration.
	 * 
	 * @param enumClassName the class of the enumeration
	 * @param name the name of the enumeration element
	 */
	public EnumValue(String enumClassName, String name) {
		Objects.requireNonNull(enumClassName, "enumClassName cannot be null");
		Objects.requireNonNull(name, "name cannot be null");
		this.enumClassName = enumClassName;
		this.name = name;
	}

	@Override
	public String toString() {
		return String.format("%s.%s", enumClassName, name);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EnumValue && ((EnumValue) other).name.equals(name)
			&& ((EnumValue) other).enumClassName.equals(enumClassName);
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ enumClassName.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;

		diff = enumClassName.compareTo(((EnumValue) other).enumClassName);
		if (diff != 0)
			return diff;
		else
			return name.compareTo(((EnumValue) other).name);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeStringUnshared(enumClassName);
		context.writeStringUnshared(name);
	}
}