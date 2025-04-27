/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.internal.json;

import java.math.BigInteger;

import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.ByteValue;
import io.hotmoka.node.api.values.CharValue;
import io.hotmoka.node.api.values.DoubleValue;
import io.hotmoka.node.api.values.FloatValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.LongValue;
import io.hotmoka.node.api.values.NullValue;
import io.hotmoka.node.api.values.ShortValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.internal.values.AbstractStorageValue;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link StorageValue}.
 */
public abstract class StorageValueJson implements JsonRepresentation<StorageValue> {
	private final BigInteger bigIntegerValue;
	private final Boolean booleanValue;
	private final Byte byteValue;
	private final Character charValue;
	private final Double doubleValue;
	private final Float floatValue;
	private final Integer intValue;
	private final Long longValue;
	private final Boolean nullValue;
	private final Short shortValue;
	private final TransactionReferences.Json transaction;
	private final BigInteger progressive;
	private final String stringValue;

	public BigInteger getBigIntegerValue() {
		return bigIntegerValue;
	}

	public Boolean getBooleanValue() {
		return booleanValue;
	}

	public Byte getByteValue() {
		return byteValue;
	}

	public Character getCharValue() {
		return charValue;
	}

	public Double getDoubleValue() {
		return doubleValue;
	}

	public Float getFloatValue() {
		return floatValue;
	}

	public Integer getIntValue() {
		return intValue;
	}

	public Long getLongValue() {
		return longValue;
	}

	public boolean isNullValue() {
		return Boolean.TRUE.equals(nullValue);
	}

	public Short getShortValue() {
		return shortValue;
	}

	public TransactionReferences.Json getTransaction() {
		return transaction;
	}

	public BigInteger getProgressive() {
		return progressive;
	}

	public String getStringValue() {
		return stringValue;
	}

	protected StorageValueJson(StorageValue value) {
		bigIntegerValue = value instanceof BigIntegerValue biv ? biv.getValue() : null;
		booleanValue = value instanceof BooleanValue bv ? bv.getValue() : null;
		byteValue = value instanceof ByteValue bv ? bv.getValue() : null;
		charValue = value instanceof CharValue cv ? cv.getValue() : null;
		doubleValue = value instanceof DoubleValue dv ? dv.getValue() : null;
		floatValue = value instanceof FloatValue fv ? fv.getValue() : null;
		intValue = value instanceof IntValue iv ? iv.getValue() : null;
		longValue = value instanceof LongValue lv ? lv.getValue() : null;
		nullValue = value instanceof NullValue ? Boolean.TRUE : null;
		shortValue = value instanceof ShortValue sv ? sv.getValue() : null;
		transaction = value instanceof StorageReference sr ? new TransactionReferences.Json(sr.getTransaction()) : null;
		progressive = value instanceof StorageReference sr ? sr.getProgressive() : null;
		stringValue = value instanceof StringValue sv ? sv.getValue() : null;

		if (bigIntegerValue == null && booleanValue == null && byteValue == null && charValue == null &&
				doubleValue == null && floatValue == null && intValue == null && longValue == null &&
				nullValue == null && shortValue == null && transaction == null && progressive == null && stringValue == null)
			throw new RuntimeException("Unexpected storage value of class " + value.getClass().getName());
	}

	@Override
	public StorageValue unmap() throws InconsistentJsonException {
		return AbstractStorageValue.from(this);
	}
}