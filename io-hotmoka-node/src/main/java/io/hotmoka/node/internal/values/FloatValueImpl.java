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
import io.hotmoka.node.api.values.FloatValue;
import io.hotmoka.node.api.values.StorageValue;

/**
 * Implementation of a {@code float} value stored in blockchain.
 */
@Immutable
public final class FloatValueImpl extends AbstractStorageValue implements FloatValue {
	static final byte SELECTOR = 5;

	/**
	 * The value.
	 */
	private final float value;

	/**
	 * Builds a {@code float} value.
	 * 
	 * @param value the value
	 */
	public FloatValueImpl(float value) {
		this.value = value;
	}

	@Override
	public float getValue() {
		return value;
	}

	@Override
	public String toString() {
		return Float.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FloatValue fv && fv.getValue() == value;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		if (other instanceof FloatValue fv)
			return Float.compare(value, fv.getValue());
		else
			return super.compareTo(other);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeFloat(value);
	}
}