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
import io.hotmoka.beans.api.values.LongValue;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * Implementation of a {@code long} value stored in blockchain.
 */
@Immutable
public final class LongValueImpl extends StorageValueImpl implements LongValue {
	static final byte SELECTOR = 7;

	/**
	 * The value.
	 */
	private final long value;

	/**
	 * Builds a {@code long} value.
	 * 
	 * @param value the value
	 */
	public LongValueImpl(long value) {
		this.value = value;
	}

	@Override
	public long getValue() {
		return value;
	}

	@Override
	public String toString() {
		return Long.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof LongValue lv && lv.getValue() == value;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Long.compare(value, ((LongValueImpl) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeLong(value);
	}
}