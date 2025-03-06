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
import io.hotmoka.node.api.values.CharValue;
import io.hotmoka.node.api.values.StorageValue;

/**
 * Implementation of a {@code char} value stored in a Hotmoka node.
 */
@Immutable
public final class CharValueImpl extends AbstractStorageValue implements CharValue {
	static final byte SELECTOR = 3;

	/**
	 * The value.
	 */
	private final char value;

	/**
	 * Builds a {@code char} value.
	 * 
	 * @param value the value
	 */
	public CharValueImpl(char value) {
		this.value = value;
	}

	@Override
	public char getValue() {
		return value;
	}

	@Override
	public String toString() {
		return Character.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CharValue cv && cv.getValue() == value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public int compareTo(StorageValue other) {
		if (other instanceof CharValue cv)
			return Character.compare(value, cv.getValue());
		else
			return super.compareTo(other);
	}

	@Override
	public <E extends Exception> char asChar(Function<StorageValue, ? extends E> exception) {
		return value;
	}

	@Override
	public <E extends Exception> char asReturnedChar(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		return value;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeChar(value);
	}
}