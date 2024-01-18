/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

package io.hotmoka.network.updates;

import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.api.values.ByteValue;
import io.hotmoka.beans.api.values.CharValue;
import io.hotmoka.beans.api.values.DoubleValue;
import io.hotmoka.beans.api.values.EnumValue;
import io.hotmoka.beans.api.values.FloatValue;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.LongValue;
import io.hotmoka.beans.api.values.ShortValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.UpdateOfBigInteger;
import io.hotmoka.beans.updates.UpdateOfBoolean;
import io.hotmoka.beans.updates.UpdateOfByte;
import io.hotmoka.beans.updates.UpdateOfChar;
import io.hotmoka.beans.updates.UpdateOfDouble;
import io.hotmoka.beans.updates.UpdateOfEnumEager;
import io.hotmoka.beans.updates.UpdateOfEnumLazy;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.updates.UpdateOfFloat;
import io.hotmoka.beans.updates.UpdateOfInt;
import io.hotmoka.beans.updates.UpdateOfLong;
import io.hotmoka.beans.updates.UpdateOfShort;
import io.hotmoka.beans.updates.UpdateOfStorage;
import io.hotmoka.beans.updates.UpdateOfString;
import io.hotmoka.beans.updates.UpdateToNullEager;
import io.hotmoka.beans.updates.UpdateToNullLazy;
import io.hotmoka.network.signatures.FieldSignatureModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.StorageValueModel;
import io.hotmoka.network.values.TransactionReferenceModel;

/**
 * The model of an update of an object.
 */
public class UpdateModel {

	/**
	 * The field that is updated. This is {@code null} for class tags.
	 */
	public FieldSignatureModel field;

	/**
	 * The value assigned to the updated field. This is {@code null} for class tags.
	 */
	public StorageValueModel value;

	/**
	 * The name of the class of the object. This is non-{@code null} for class tags only.
	 */
	public String className;

	/**
	 * The transaction that installed the jar from where the class has been loaded.
	 * This is non-{@code null} for class tags only.
	 */
	public TransactionReferenceModel jar;

	/**
	 * The object whose field is modified.
	 */
	public StorageReferenceModel object;

	/**
	 * Builds the model of an update of an object.
	 * 
	 * @param update the update
	 */
	public UpdateModel(Update update) {
		this.object = new StorageReferenceModel(update.getObject());

		if (update instanceof ClassTag) {
			ClassTag classTag = (ClassTag) update;

			this.field = null;
			this.value = null;
			this.className = classTag.clazz.getName();
			this.jar = new TransactionReferenceModel(classTag.jar);
		}
		else {
			UpdateOfField updateOfField = (UpdateOfField) update;

			this.field = new FieldSignatureModel(updateOfField.getField());
			this.value = new StorageValueModel(updateOfField.getValue());
			this.className = null;
			this.jar = null;
		}
	}

	public UpdateModel() {}

	/**
	 * Yields the update having this model.
	 * 
	 * @return the update
	 */
	public Update toBean() {
		if (object == null)
			throw new RuntimeException("Unexpected null update object");
		else if (className != null)
			return new ClassTag(object.toBean(), className, jar.toBean());
		else {
			FieldSignature field = this.field.toBean();
			StorageValue value = this.value.toBean();
			StorageReference object = this.object.toBean();

			if (value == StorageValues.NULL)
				if (field.getType().isEager())
					return new UpdateToNullEager(object, field);
				else
					return new UpdateToNullLazy(object, field);
			else if (value instanceof EnumValue ev)
				if (field.getType().isEager())
					return new UpdateOfEnumEager(object, field, ev.getEnumClassName(), ev.getName());
				else
					return new UpdateOfEnumLazy(object, field, ev.getEnumClassName(), ev.getName());
			else if (value instanceof BigIntegerValue biv)
				return new UpdateOfBigInteger(object, field, biv.getValue());
			else if (value instanceof StringValue sv)
				return new UpdateOfString(object, field, sv.getValue());
			else if (value instanceof StorageReference sr)
				return new UpdateOfStorage(object, field, sr);
			else if (value instanceof BooleanValue bv)
				return new UpdateOfBoolean(object, field, bv.getValue());
			else if (value instanceof ByteValue bv)
				return new UpdateOfByte(object, field, bv.getValue());
			else if (value instanceof CharValue cv)
				return new UpdateOfChar(object, field, cv.getValue());
			else if (value instanceof DoubleValue dv)
				return new UpdateOfDouble(object, field, dv.getValue());
			else if (value instanceof FloatValue fv)
				return new UpdateOfFloat(object, field, fv.getValue());
			else if (value instanceof IntValue iv)
				return new UpdateOfInt(object, field, iv.getValue());
			else if (value instanceof LongValue lv)
				return new UpdateOfLong(object, field, lv.getValue());
			else if (value instanceof ShortValue sv)
				return new UpdateOfShort(object, field, sv.getValue());
			else
				throw new RuntimeException("Unexpected update value of class " + value.getClass().getName());
		}
	}
}