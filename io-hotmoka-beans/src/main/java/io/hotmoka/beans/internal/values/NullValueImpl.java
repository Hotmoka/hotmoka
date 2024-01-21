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
import io.hotmoka.beans.api.values.NullValue;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * Implemenytation of the {@code null} value stored in blockchain.
 */
@Immutable
public final class NullValueImpl extends AbstractStorageValue implements NullValue {
	static final byte SELECTOR = 8;

	/**
	 * The {@code null} value.
	 */
	public final static NullValue NULL = new NullValueImpl();

	/**
	 * Builds the {@code null} value.
	 */
	private NullValueImpl() {}

	@Override
	public String toString() {
		return "null";
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof NullValue;
	}

	@Override
	public int hashCode() {
		return 13011973;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
	}

	@Override
	public int compareTo(StorageValue other) {
		return other instanceof NullValue ? 0 : super.compareTo(other);
	}
}