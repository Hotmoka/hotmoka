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
import io.hotmoka.node.api.values.DoubleValue;
import io.hotmoka.node.api.values.StorageValue;

/**
 * Implementation of a {@code double} value stored in a Hotmoka node.
 */
@Immutable
public final class DoubleValueImpl extends AbstractStorageValue implements DoubleValue {
	static final byte SELECTOR = 4;

	/**
	 * The value.
	 */
	private final double value;

	/**
	 * Builds a {@code double} value.
	 * 
	 * @param value the value
	 */
	public DoubleValueImpl(double value) {
		this.value = value;
	}

	@Override
	public double getValue() {
		return value;
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof DoubleValue dv && dv.getValue() == value;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		if (other instanceof DoubleValue dv)
			return Double.compare(value, dv.getValue());
		else
			return super.compareTo(other);
	}

	@Override
	public <E extends Exception> double asDouble(Function<StorageValue, ? extends E> exception) {
		return value;
	}

	@Override
	public <E extends Exception> double asReturnedDouble(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		return value;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeDouble(value);
	}
}