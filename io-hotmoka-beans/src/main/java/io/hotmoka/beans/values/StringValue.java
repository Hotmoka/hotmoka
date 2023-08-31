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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * A string stored in blockchain.
 */
@Immutable
public final class StringValue extends StorageValue {
	static final byte SELECTOR = 10;
	static final byte SELECTOR_EMPTY_STRING = 13;

	/**
	 * The string.
	 */
	public final String value;

	/**
	 * Builds a string that can be stored in blockchain.
	 * 
	 * @param value the string
	 */
	public StringValue(String value) {
		if (value == null)
			throw new IllegalArgumentException("value cannot be null");

		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StringValue && ((StringValue) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((StringValue) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if ("".equals(value))
			context.writeByte(SELECTOR_EMPTY_STRING);
		else {
			context.writeByte(SELECTOR);
			context.writeStringUnshared(value);
		}
	}
}