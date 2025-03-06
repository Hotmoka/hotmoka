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
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.StorageValue;

/**
 * Implementation of a {@code boolean} value stored in a Hotmoka node.
 */
@Immutable
public final class BooleanValueImpl extends AbstractStorageValue implements BooleanValue {
	static final byte SELECTOR_TRUE = 0;
	static final byte SELECTOR_FALSE = 1;

	/**
	 * The true Boolean value.
	 */
	public final static BooleanValue TRUE = new BooleanValueImpl(true);

	/**
	 * The false Boolean value.
	 */
	public final static BooleanValue FALSE = new BooleanValueImpl(false);

	/**
	 * The value.
	 */
	private final boolean value;

	/**
	 * Builds a {@code boolean} value.
	 * 
	 * @param value the value
	 */
	private BooleanValueImpl(boolean value) {
		this.value = value;
	}

	@Override
	public boolean getValue() {
		return value;
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BooleanValue bv && bv.getValue() == value;
	}

	@Override
	public int hashCode() {
		return Boolean.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		if (other instanceof BooleanValue bv)
			return Boolean.compare(value, bv.getValue());
		else
			return super.compareTo(other);
	}

	@Override
	public <E extends Exception> boolean asBoolean(Function<StorageValue, ? extends E> exception) {
		return value;
	}

	@Override
	public <E extends Exception> boolean asReturnedBoolean(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		return value;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(value ? SELECTOR_TRUE : SELECTOR_FALSE);
	}
}