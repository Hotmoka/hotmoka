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
 * An {@code int} value stored in the store of a node.
 */
@Immutable
public final class IntValue extends StorageValue {
	static final byte SELECTOR = 14;

	/**
	 * The value.
	 */
	public final int value;

	/**
	 * Builds an {@code int} value.
	 * 
	 * @param value the value
	 */
	public IntValue(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof IntValue iv && iv.value == value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return Integer.compare(value, ((IntValue) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (value >= 0 && value < 255 - SELECTOR)
			context.writeByte(SELECTOR + 1 + value);
		else {
			context.writeByte(SELECTOR);
			context.writeInt(value);
		}
	}
}