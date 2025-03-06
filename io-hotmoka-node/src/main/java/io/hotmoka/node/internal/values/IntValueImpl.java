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
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageValue;

/**
 * An {@code int} value stored in the store of a Hotmoka node.
 */
@Immutable
public final class IntValueImpl extends AbstractStorageValue implements IntValue {
	static final byte SELECTOR = 13;

	/**
	 * The value.
	 */
	private final int value;

	/**
	 * Builds an {@code int} value.
	 * 
	 * @param value the value
	 */
	public IntValueImpl(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof IntValue iv && iv.getValue() == value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public int compareTo(StorageValue other) {
		if (other instanceof IntValue iv)
			return Integer.compare(value, iv.getValue());
		else
			return super.compareTo(other);
	}

	@Override
	public <E extends Exception> int asInt(Function<StorageValue, ? extends E> exception) {
		return value;
	}

	@Override
	public <E extends Exception> int asReturnedInt(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		return value;
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