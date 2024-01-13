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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.values.ByteValue;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * Implementation of a {@code byte} value stored in blockchain.
 */
@Immutable
public final class ByteValueImpl extends StorageValueImpl implements ByteValue {
	static final byte SELECTOR = 2;

	/**
	 * The value.
	 */
	private final byte value;

	/**
	 * Builds a {@code byte} value.
	 * 
	 * @param value the value
	 */
	public ByteValueImpl(byte value) {
		this.value = value;
	}

	@Override
	public byte getValue() {
		return value;
	}

	@Override
	public String toString() {
		return Byte.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ByteValue bv && bv.getValue() == value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Byte.compare(value, ((ByteValueImpl) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeByte(value);
	}
}