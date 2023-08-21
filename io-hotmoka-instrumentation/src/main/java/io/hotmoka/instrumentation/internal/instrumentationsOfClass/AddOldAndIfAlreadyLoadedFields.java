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

package io.hotmoka.instrumentation.internal.instrumentationsOfClass;

import java.lang.reflect.Field;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.Type;

import io.hotmoka.instrumentation.InstrumentationFields;
import io.hotmoka.instrumentation.internal.InstrumentationConstants;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.ClassLevelInstrumentation;

/**
 * An instrumentation that adds fields for the old value and the loading state of the fields of a storage class.
 */
public class AddOldAndIfAlreadyLoadedFields extends ClassLevelInstrumentation {
	private final static short PRIVATE_SYNTHETIC_TRANSIENT = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC | Const.ACC_TRANSIENT;

	/**
	 * Builds the instrumentation.
	 * 
	 * @param builder the builder of the class being instrumented
	 */
	public AddOldAndIfAlreadyLoadedFields(InstrumentedClassImpl.Builder builder) {
		builder.super();

		if (isStorage) {
			eagerNonTransientInstanceFields.getLast().forEach(this::addOldFieldFor);
			lazyNonTransientInstanceFields.forEach(this::addOldFieldFor);
			lazyNonTransientInstanceFields.forEach(this::addIfAlreadyLoadedFieldFor);
		}
	}

	/**
	 * Adds the field for the old value of a field of a storage class.
	 * 
	 * @param field the field of the storage class
	 */
	private void addOldFieldFor(Field field) {
		addField(new FieldGen(PRIVATE_SYNTHETIC_TRANSIENT, Type.getType(field.getType()), InstrumentationFields.OLD_PREFIX + field.getName(), cpg).getField());
	}

	/**
	 * Adds the field for the loading state of a field of a storage class.
	 * 
	 * @param field the field of the storage class
	 */
	private void addIfAlreadyLoadedFieldFor(Field field) {
		addField(new FieldGen(PRIVATE_SYNTHETIC_TRANSIENT, BasicType.BOOLEAN, InstrumentationConstants.IF_ALREADY_LOADED_PREFIX + field.getName(), cpg).getField());
	}
}