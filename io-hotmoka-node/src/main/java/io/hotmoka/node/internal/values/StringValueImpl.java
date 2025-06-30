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
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.internal.json.StorageValueJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of a string stored in blockchain.
 */
@Immutable
public final class StringValueImpl extends AbstractStorageValue implements StringValue {
	static final byte SELECTOR = 10;
	static final byte SELECTOR_EMPTY_STRING = 12;

	/**
	 * The string.
	 */
	private final String value;

	/**
	 * Builds a string value that can be stored in a Hotmoka node.
	 * 
	 * @param value the string
	 */
	public StringValueImpl(String value) {
		this(value, IllegalArgumentException::new);
	}

	/**
	 * Unmarshals a string value from the given stream.
	 * The selector of the value has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @throws IOException if the response could not be unmarshalled
	 */
	public StringValueImpl(UnmarshallingContext context, byte selector) throws IOException {
		this(unmarshalValue(context, selector), IOException::new);
	}

	private static String unmarshalValue(UnmarshallingContext context, byte selector) throws IOException {
		switch (selector) {
		case SELECTOR_EMPTY_STRING: return "";
		case SELECTOR: return context.readStringUnshared();
		default: throw new IllegalArgumentException("Unexpected selector " + selector + " for a string value");
		}
	}

	/**
	 * Creates a string value from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public StringValueImpl(StorageValueJson json) throws InconsistentJsonException {
		this(json.getStringValue(), InconsistentJsonException::new);
	}

	/**
	 * Builds a string that can be stored in blockchain.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param value the string
	 * @param onIllegalArgs the supplier of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> StringValueImpl(String value, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		this.value = Objects.requireNonNull(value, "value cannot be null", onIllegalArgs);
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StringValue sv && sv.getValue().equals(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		if (other instanceof StringValue sv)
			return value.compareTo(sv.getValue());
		else
			return super.compareTo(other);
	}

	@Override
	public <E extends Exception> String asString(Function<StorageValue, ? extends E> exception) {
		return value;
	}

	@Override
	public <E extends Exception> String asReturnedString(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		return value;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (value.isEmpty())
			context.writeByte(SELECTOR_EMPTY_STRING);
		else {
			context.writeByte(SELECTOR);
			context.writeStringUnshared(value);
		}
	}
}