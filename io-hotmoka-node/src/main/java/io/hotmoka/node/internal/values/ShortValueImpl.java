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

package io.hotmoka.node.internal.values;

import java.io.IOException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.api.values.ShortValue;
import io.hotmoka.node.api.values.StorageValue;

/**
 * Implementation of a {@code short} value stored in blockchain.
 */
@Immutable
public final class ShortValueImpl extends AbstractStorageValue implements ShortValue {
	static final byte SELECTOR = 9;

	/**
	 * The value.
	 */
	public final short value;

	/**
	 * Builds a {@code short} value.
	 * 
	 * @param value the value
	 */
	public ShortValueImpl(short value) {
		this.value = value;
	}

	@Override
	public short getValue() {
		return value;
	}

	@Override
	public String toString() {
		return Short.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ShortValue sv && sv.getValue() == value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public int compareTo(StorageValue other) {
		if (other instanceof ShortValue sv)
			return Short.compare(value, sv.getValue());
		else
			return super.compareTo(other);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeShort(value);
	}
}