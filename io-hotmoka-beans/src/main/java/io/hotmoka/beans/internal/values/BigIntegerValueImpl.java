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
import java.math.BigInteger;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * Implementation of a big integer stored in blockchain.
 */
@Immutable
public final class BigIntegerValueImpl extends AbstractStorageValue implements BigIntegerValue {
	static final byte SELECTOR = 6;

	/**
	 * The big integer.
	 */
	private final BigInteger value;

	/**
	 * Builds a big integer that can be stored in blockchain.
	 * 
	 * @param value the big integer
	 */
	public BigIntegerValueImpl(BigInteger value) {
		this.value = Objects.requireNonNull(value, "value cannot be null");
	}

	@Override
	public BigInteger getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BigIntegerValue biv && biv.getValue().equals(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		if (other instanceof BigIntegerValue biv)
			return value.compareTo(biv.getValue());
		else
			return super.compareTo(other);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeBigInteger(value);
	}
}