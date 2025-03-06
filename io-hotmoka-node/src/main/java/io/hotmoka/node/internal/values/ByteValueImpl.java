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
import java.util.function.Function;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.values.ByteValue;
import io.hotmoka.node.api.values.StorageValue;

/**
 * Implementation of a {@code byte} value stored in a Hotmoka node.
 */
@Immutable
public final class ByteValueImpl extends AbstractStorageValue implements ByteValue {
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
		if (other instanceof ByteValue bv)
			return Byte.compare(value, bv.getValue());
		else
			return super.compareTo(other);
	}

	@Override
	public <E extends Exception> byte asByte(Function<StorageValue, ? extends E> exception) {
		return value;
	}

	@Override
	public <E extends Exception> byte asReturnedByte(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		return value;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeByte(value);
	}
}