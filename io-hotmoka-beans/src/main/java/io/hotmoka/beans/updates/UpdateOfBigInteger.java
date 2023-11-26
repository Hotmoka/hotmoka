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

package io.hotmoka.beans.updates;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * An update of a field states that the {@link java.math.BigInteger}
 * field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfBigInteger extends UpdateOfField {
	final static byte SELECTOR = 2;
	final static byte SELECTOR_BALANCE = 1;
	final static byte SELECTOR_NONCE = 12;
	final static byte SELECTOR_RED_BALANCE = 13;
	final static byte SELECTOR_RED_BALANCE_TO_ZERO = 14;
	final static byte SELECTOR_GAS_PRICE = 37;
	final static byte SELECTOR_UBI_VALUE = 38;
	final static byte SELECTOR_BALANCE_TO_ZERO = 39;
	final static byte SELECTOR_NONCE_TO_ZERO = 40;

	/**
	 * The new value of the field.
	 */
	public final BigInteger value;

	/**
	 * Builds an update of a {@link java.math.BigInteger} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfBigInteger(StorageReference object, FieldSignature field, BigInteger value) {
		super(object, field);

		Objects.requireNonNull(value, "value cannot be null");
		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new BigIntegerValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfBigInteger && super.equals(other) && ((UpdateOfBigInteger) other).value.equals(value);
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
			return value.compareTo(((UpdateOfBigInteger) other).value);
	}

	@Override
	public boolean isEager() {
		// a lazy BigInteger could be stored into a lazy Object or Serializable or Comparable or Number field
		return field.type.equals(ClassType.BIG_INTEGER);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (FieldSignature.RED_BALANCE_FIELD.equals(field) && value.signum() == 0) {
			// this case is frequent, since most contracts do not use the red balance, that remains at 0
			context.writeByte(SELECTOR_RED_BALANCE_TO_ZERO);
			super.intoWithoutField(context);
			return; // note this
		}
		else if (FieldSignature.BALANCE_FIELD.equals(field) && value.signum() == 0) {
			// this case is frequent, since most contracts have zero balance
			context.writeByte(SELECTOR_BALANCE_TO_ZERO);
			super.intoWithoutField(context);
			return; // note this
		}
		else if (FieldSignature.EOA_NONCE_FIELD.equals(field) && value.signum() == 0) {
			// this case is frequent, since EOAs starts with nonce at zero
			context.writeByte(SELECTOR_NONCE_TO_ZERO);
			super.intoWithoutField(context);
			return; // note this
		}
		else if (FieldSignature.BALANCE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_BALANCE);
			super.intoWithoutField(context);
		}
		else if (FieldSignature.RED_BALANCE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_RED_BALANCE);
			super.intoWithoutField(context);
		}
		else if (FieldSignature.EOA_NONCE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_NONCE);
			super.intoWithoutField(context);
		}
		else if (FieldSignature.GENERIC_GAS_STATION_GAS_PRICE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_GAS_PRICE);
			super.intoWithoutField(context);
		}
		else if (FieldSignature.UNSIGNED_BIG_INTEGER_VALUE_FIELD.equals(field)) {
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