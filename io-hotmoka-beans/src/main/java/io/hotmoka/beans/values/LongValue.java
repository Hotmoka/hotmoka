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
import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.BeanMarshallingContext;

/**
 * A {@code long} value stored in blockchain.
 */
@Immutable
public final class LongValue extends StorageValue {
	static final byte SELECTOR = 7;

	/**
	 * The value.
	 */
	public final long value;

	/**
	 * Builds a {@code long} value.
	 * 
	 * @param value the value
	 */
	public LongValue(long value) {
		this.value = value;
	}

	/**
	 * Builds a {@code long} value.
	 * 
	 * @param value the value
	 */
	public LongValue(BigInteger value) {
		this.value = value.longValue();
		if (!BigInteger.valueOf(this.value).equals(value))
			throw new IllegalArgumentException("value is too big for a long");
	}

	@Override
	public String toString() {
		return Long.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof LongValue && ((LongValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return Long.compare(value, ((LongValue) other).value);
	}

	@Override
	public void into(BeanMarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeLong(value);
	}
}