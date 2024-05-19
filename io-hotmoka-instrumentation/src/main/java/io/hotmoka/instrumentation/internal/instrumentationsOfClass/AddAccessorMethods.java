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
import java.lang.reflect.Modifier;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import io.hotmoka.instrumentation.internal.InstrumentationConstants;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.ClassLevelInstrumentation;

/**
 * An instrumentation that adds accessor methods for the fields of the class being instrumented.
 */
public class AddAccessorMethods extends ClassLevelInstrumentation {
	private final static short PUBLIC_SYNTHETIC_FINAL = Const.ACC_PUBLIC | Const.ACC_SYNTHETIC | Const.ACC_FINAL;

	/**
	 * Builds the instrumentation.
	 * 
	 * @param builder the builder of the class being instrumented
	 */
	public AddAccessorMethods(InstrumentedClassImpl.Builder builder) {
		builder.super();

		if (isStorage)
			lazyNonTransientInstanceFields.forEach(this::addAccessorMethodsFor);
	}

	/**
	 * Adds accessor methods for the given field.
	 * 
	 * @param field the field
	 */
	private void addAccessorMethodsFor(Field field) {
		addGetterFor(field);

		if (!Modifier.isFinal(field.getModifiers()))
			addSetterFor(field);
	}

	/**
	 * Adds a setter method for the given field.
	 * 
	 * @param field the field
	 */
	private void addSetterFor(Field field) {
		var type = Type.getType(field.getType());
		var il = new InstructionList();
		il.append(InstructionFactory.createThis());
		il.append(InstructionConst.DUP);
		il.append(factory.createInvoke(className, InstrumentationConstants.ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
		il.append(InstructionConst.ALOAD_1);
		il.append(factory.createPutField(className, field.getName(), type));
		il.append(InstructionConst.RETURN);

		var setter = new MethodGen(PUBLIC_SYNTHETIC_FINAL, BasicType.VOID, new Type[] { type }, null, setterNameFor(className, field.getName()), className, il, cpg);
		addMethod(setter, false);
	}

	/**
	 * Adds a getter method for the given field.
	 * 
	 * @param field the field
	 */
	private void addGetterFor(Field field) {
		var type = Type.getType(field.getType());
		var il = new InstructionList();
		il.append(InstructionFactory.createThis());
		il.append(InstructionConst.DUP);
		il.append(factory.createInvoke(className, InstrumentationConstants.ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
		il.append(factory.createGetField(className, field.getName(), type));
		il.append(InstructionFactory.createReturn(type));

		var getter = new MethodGen(PUBLIC_SYNTHETIC_FINAL, type, Type.NO_ARGS, null, getterNameFor(className, field.getName()), className, il, cpg);
		addMethod(getter, false);
	}
}