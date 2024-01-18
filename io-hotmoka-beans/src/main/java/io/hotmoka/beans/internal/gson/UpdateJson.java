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

package io.hotmoka.beans.internal.gson;

import io.hotmoka.beans.FieldSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.Updates;
import io.hotmoka.beans.api.updates.ClassTag;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.updates.UpdateOfEnum;
import io.hotmoka.beans.api.updates.UpdateOfField;
import io.hotmoka.beans.api.updates.UpdateToNull;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.api.values.ByteValue;
import io.hotmoka.beans.api.values.CharValue;
import io.hotmoka.beans.api.values.DoubleValue;
import io.hotmoka.beans.api.values.EnumValue;
import io.hotmoka.beans.api.values.FloatValue;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.LongValue;
import io.hotmoka.beans.api.values.NullValue;
import io.hotmoka.beans.api.values.ShortValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link Update}.
 */
public abstract class UpdateJson implements JsonRepresentation<Update> {

	/**
	 * The updated object: this always exists.
	 */
	private final StorageValues.Json object;

	/**
	 * The updated field: this exists only for updates of fields.
	 */
	private final FieldSignatures.Json field;

	/**
	 * The value written into {@link #field}: this exists only for updates of fields.
	 */
	private final StorageValues.Json value;

	/**
	 * The class of the {@link #object}: this exists only for a class tag.
	 */
	private final String clazz;

	/**
	 * The reference to the transaction that installed the class of {@link #object}:
	 * this exists only for a class tag.
	 */
	private final TransactionReferences.Json jar;

	/**
	 * True if and only if the update is eager: this exists only for updates of fields
	 * of enumeration type or for updates to {@code null}.
	 */
	private final Boolean eager;

	protected UpdateJson(Update update) {
		this.object = new StorageValues.Json(update.getObject());

		if (update instanceof ClassTag ct) {
			this.clazz = ct.getClazz().getName();
			this.jar = new TransactionReferences.Json(ct.getJar());
			this.field = null;
			this.value = null;
			this.eager = null;
		}
		else if (update instanceof UpdateOfField uof) {
			this.field = new FieldSignatures.Json(uof.getField());
			this.value = new StorageValues.Json(uof.getValue());

			if (update instanceof UpdateOfEnum uoe)
				this.eager = uoe.isEager();
			else if (update instanceof UpdateToNull utn)
				this.eager = utn.isEager();
			else
				this.eager = null;

			this.clazz = null;
			this.jar = null;
		}
		else
			throw new IllegalArgumentException("Unexpected update of class " + update.getClass().getName());
	}

	@Override
	public Update unmap() throws IllegalArgumentException, HexConversionException {
		var object = (StorageReference) this.object.unmap();

		if (clazz != null)
			return Updates.classTag(object, StorageTypes.classNamed(clazz), jar.unmap());

		var field = this.field.unmap();
		var value = this.value.unmap();

		if (value instanceof BigIntegerValue biv)
			return Updates.ofBigInteger(object, field, biv.getValue());
		else if (value instanceof BooleanValue bv)
			return Updates.ofBoolean(object, field, bv.getValue());
		else if (value instanceof ByteValue bv)
			return Updates.ofByte(object, field, bv.getValue());
		else if (value instanceof CharValue cv)
			return Updates.ofChar(object, field, cv.getValue());
		else if (value instanceof DoubleValue dv)
			return Updates.ofDouble(object, field, dv.getValue());
		else if (value instanceof EnumValue ev)
			return Updates.ofEnum(object, field, ev.getEnumClassName(), ev.getName(), eager);
		else if (value instanceof FloatValue fv)
			return Updates.ofFloat(object, field, fv.getValue());
		else if (value instanceof IntValue iv)
			return Updates.ofInt(object, field, iv.getValue());
		else if (value instanceof LongValue lv)
			return Updates.ofLong(object, field, lv.getValue());
		else if (value instanceof ShortValue sv)
			return Updates.ofShort(object, field, sv.getValue());
		else if (value instanceof StorageReference sr)
			return Updates.ofStorage(object, field, sr);
		else if (value instanceof StringValue sv)
			return Updates.ofString(object, field, sv.getValue());
		else if (value instanceof NullValue)
			return Updates.toNull(object, field, eager);
		else
			throw new IllegalArgumentException("Illegal update JSON");
	}
}