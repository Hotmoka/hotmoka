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

import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.Updates;
import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.beans.api.updates.ClassTag;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.updates.UpdateOfField;
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
			this.className = classTag.getClazz().getName();
			this.jar = new TransactionReferenceModel(classTag.getJar());
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
			return Updates.classTag(object.toBean(), StorageTypes.classNamed(className), jar.toBean());
		else {
			FieldSignature field = this.field.toBean();
			StorageValue value = this.value.toBean();
			StorageReference object = this.object.toBean();

			if (value == StorageValues.NULL)
				return Updates.toNull(object, field, field.getType().isEager());
			else if (value instanceof EnumValue ev)
				return Updates.ofEnum(object, field, ev.getEnumClassName(), ev.getName(), field.getType().isEager());
			else if (value instanceof BigIntegerValue biv)
				return Updates.ofBigInteger(object, field, biv.getValue());
			else if (value instanceof StringValue sv)
				return Updates.ofString(object, field, sv.getValue());
			else if (value instanceof StorageReference sr)
				return Updates.ofStorage(object, field, sr);
			else if (value instanceof BooleanValue bv)
				return Updates.ofBoolean(object, field, bv.getValue());
			else if (value instanceof ByteValue bv)
				return Updates.ofByte(object, field, bv.getValue());
			else if (value instanceof CharValue cv)
				return Updates.ofChar(object, field, cv.getValue());
			else if (value instanceof DoubleValue dv)
				return Updates.ofDouble(object, field, dv.getValue());
			else if (value instanceof FloatValue fv)
				return Updates.ofFloat(object, field, fv.getValue());
			else if (value instanceof IntValue iv)
				return Updates.ofInt(object, field, iv.getValue());
			else if (value instanceof LongValue lv)
				return Updates.ofLong(object, field, lv.getValue());
			else if (value instanceof ShortValue sv)
				return Updates.ofShort(object, field, sv.getValue());
			else
				throw new RuntimeException("Unexpected update value of class " + value.getClass().getName());
		}
	}
}