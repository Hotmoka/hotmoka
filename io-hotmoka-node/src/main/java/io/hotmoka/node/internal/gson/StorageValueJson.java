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

package io.hotmoka.node.internal.gson;

import java.math.BigInteger;

import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.ByteValue;
import io.hotmoka.node.api.values.CharValue;
import io.hotmoka.node.api.values.DoubleValue;
import io.hotmoka.node.api.values.EnumValue;
import io.hotmoka.node.api.values.FloatValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.LongValue;
import io.hotmoka.node.api.values.NullValue;
import io.hotmoka.node.api.values.ShortValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link StorageValue}.
 */
public abstract class StorageValueJson implements JsonRepresentation<StorageValue> {
	private BigInteger bigIntegerValue;
	private Boolean booleanValue;
	private Byte byteValue;
	private Character charValue;
	private Double doubleValue;
	private String enumClass;
	private String name;
	private Float floatValue;
	private Integer intValue;
	private Long longValue;
	private Boolean nullValue;
	private Short shortValue;
	private TransactionReferences.Json transaction;
	private BigInteger progressive;
	private String stringValue;

	protected StorageValueJson(StorageValue value) {
		if (value instanceof BigIntegerValue biv)
			bigIntegerValue = biv.getValue();
		else if (value instanceof BooleanValue bv)
			booleanValue = bv.getValue();
		else if (value instanceof ByteValue bv)
			byteValue = bv.getValue();
		else if (value instanceof CharValue cv)
			charValue = cv.getValue();
		else if (value instanceof DoubleValue dv)
			doubleValue = dv.getValue();
		else if (value instanceof EnumValue ev) {
			enumClass = ev.getEnumClassName();
			name = ev.getName();
		}
		else if (value instanceof FloatValue fv)
			floatValue = fv.getValue();
		else if (value instanceof IntValue iv)
			intValue = iv.getValue();
		else if (value instanceof LongValue lv)
			longValue = lv.getValue();
		else if (value instanceof NullValue)
			nullValue = true;
		else if (value instanceof ShortValue sv)
			shortValue = sv.getValue();
		else if (value instanceof StorageReference sr) {
			transaction = new TransactionReferences.Json(sr.getTransaction());
			progressive = sr.getProgressive();
		}
		else if (value instanceof StringValue sv)
			stringValue = sv.getValue();
		else if (value == null)
			throw new RuntimeException("Unexpected null storage value");
		else
			throw new RuntimeException("Unexpected storage value of class " + value.getClass().getName());
	}

	@Override
	public StorageValue unmap() throws InconsistentJsonException {
		if (bigIntegerValue != null)
			return StorageValues.bigIntegerOf(bigIntegerValue);
		else if (booleanValue != null)
			return StorageValues.booleanOf(booleanValue);
		else if (byteValue != null)
			return StorageValues.byteOf(byteValue);
		else if (charValue != null)
			return StorageValues.charOf(charValue);
		else if (doubleValue != null)
			return StorageValues.doubleOf(doubleValue);
		else if (enumClass != null && name != null)
			return StorageValues.enumElementOf(enumClass, name);
		else if (floatValue != null)
			return StorageValues.floatOf(floatValue);
		else if (intValue != null)
			return StorageValues.intOf(intValue);
		else if (longValue != null)
			return StorageValues.longOf(longValue);
		else if (nullValue != null)
			return StorageValues.NULL;
		else if (shortValue != null)
			return StorageValues.shortOf(shortValue);
		else if (transaction != null && progressive != null)
			return StorageValues.reference(transaction.unmap(), progressive);
		else if (stringValue != null)
			return StorageValues.stringOf(stringValue);
		else
			throw new InconsistentJsonException("Illegal storage value JSON");
	}
}