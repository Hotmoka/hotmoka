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
import java.math.BigInteger;
import java.util.function.Function;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.internal.json.StorageValueJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of a big integer stored in a Hotmoka node.
 */
@Immutable
public final class BigIntegerValueImpl extends AbstractStorageValue implements BigIntegerValue {
	static final byte SELECTOR = 6;

	/**
	 * The big integer.
	 */
	private final BigInteger value;

	/**
	 * Builds a big integer that can be stored in a Hotmoka node.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param value the big integer
	 * @param onIllegalArgs the supplier of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	public BigIntegerValueImpl(BigInteger value) {
		this(value, IllegalArgumentException::new);
	}

	/**
	 * Unmarshals a big integer that can be stored in a Hotmoka node, from the given stream.
	 * The selector of the value has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the response could not be unmarshalled
	 */
	public BigIntegerValueImpl(UnmarshallingContext context) throws IOException {
		this(context.readBigInteger(), IOException::new);
	}

	/**
	 * Creates a big integer that can be stored in a Hotmoka node, from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public BigIntegerValueImpl(StorageValueJson json) throws InconsistentJsonException {
		this(json.getBigIntegerValue(), InconsistentJsonException::new);
	}

	/**
	 * Builds a big integer that can be stored in a Hotmoka node.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param value the big integer
	 * @param onIllegalArgs the supplier of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> BigIntegerValueImpl(BigInteger value, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		this.value = Objects.requireNonNull(value, "value cannot be null", onIllegalArgs);
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
	public <E extends Exception> BigInteger asBigInteger(Function<StorageValue, ? extends E> exception) {
		return value;
	}

	@Override
	public <E extends Exception> BigInteger asReturnedBigInteger(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		return value;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeBigInteger(value);
	}
}