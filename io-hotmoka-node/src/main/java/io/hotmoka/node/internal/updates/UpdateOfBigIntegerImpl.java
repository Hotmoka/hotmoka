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

package io.hotmoka.node.internal.updates;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfBigInteger;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The implementation of an update of a field of type {@link java.math.BigInteger}.
 */
@Immutable
public final class UpdateOfBigIntegerImpl extends UpdateOfFieldImpl implements UpdateOfBigInteger {
	final static byte SELECTOR = 2;
	final static byte SELECTOR_BALANCE = 1;
	final static byte SELECTOR_NONCE = 12;
	final static byte SELECTOR_GAS_PRICE = 37;
	final static byte SELECTOR_UBI_VALUE = 38;
	final static byte SELECTOR_BALANCE_TO_ZERO = 39;
	final static byte SELECTOR_NONCE_TO_ZERO = 40;

	/**
	 * The new value of the field.
	 */
	private final BigInteger value;

	/**
	 * Builds an update of a {@link java.math.BigInteger} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfBigIntegerImpl(StorageReference object, FieldSignature field, BigInteger value) {
		super(object, field);

		this.value = Objects.requireNonNull(value, "value cannot be null");
	}

	@Override
	public BigIntegerValue getValue() {
		return StorageValues.bigIntegerOf(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfBigInteger uobi && super.equals(other) && uobi.getValue().getValue().equals(value);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((UpdateOfBigIntegerImpl) other).value);
	}

	@Override
	public boolean isEager() {
		// a lazy BigInteger could also be stored into a lazy Object or Serializable or Comparable or Number field
		return StorageTypes.BIG_INTEGER.equals(field.getType());
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (FieldSignatures.BALANCE_FIELD.equals(field) && value.signum() == 0) {
			// this case is frequent, since most contracts have zero balance
			context.writeByte(SELECTOR_BALANCE_TO_ZERO);
			super.intoWithoutField(context);
			return; // note this
		}
		else if (FieldSignatures.EOA_NONCE_FIELD.equals(field) && value.signum() == 0) {
			// this case is frequent, since EOAs start with nonce at zero
			context.writeByte(SELECTOR_NONCE_TO_ZERO);
			super.intoWithoutField(context);
			return; // note this
		}
		else if (FieldSignatures.BALANCE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_BALANCE);
			super.intoWithoutField(context);
		}
		else if (FieldSignatures.EOA_NONCE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_NONCE);
			super.intoWithoutField(context);
		}
		else if (FieldSignatures.GENERIC_GAS_STATION_GAS_PRICE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_GAS_PRICE);
			super.intoWithoutField(context);
		}
		else if (FieldSignatures.UNSIGNED_BIG_INTEGER_VALUE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_UBI_VALUE);
			super.intoWithoutField(context);
		}
		else {
			context.writeByte(SELECTOR);
			super.into(context);
		}

		context.writeBigInteger(value);
	}
}