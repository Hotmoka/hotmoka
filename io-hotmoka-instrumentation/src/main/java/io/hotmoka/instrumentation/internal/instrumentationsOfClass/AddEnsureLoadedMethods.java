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
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.hotmoka.instrumentation.api.InstrumentationFields;
import io.hotmoka.instrumentation.internal.InstrumentationConstants;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.ClassLevelInstrumentation;
import io.hotmoka.whitelisting.WhitelistingConstants;

/**
 * An instrumentation that adds the ensure loaded methods for the lazy fields of the class being instrumented.
 */
public class AddEnsureLoadedMethods extends ClassLevelInstrumentation {
	private final static Type[] DESERIALIZE_LAST_UPDATE_ARGS = { ObjectType.OBJECT, Type.STRING, Type.STRING, Type.STRING };
	private final static short PRIVATE_SYNTHETIC = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC;

	/**
	 * Builds the instrumentation.
	 * 
	 * @param builder the builder of the class being instrumented
	 */
	public AddEnsureLoadedMethods(InstrumentedClassImpl.Builder builder) {
		builder.super();

		if (isStorage)
			lazyNonTransientInstanceFields.forEach(this::addEnsureLoadedMethodFor);
	}

	/**
	 * Adds the ensure loaded method for the given lazy field.
	 * 
	 * @param field the field
	 */
	private void addEnsureLoadedMethodFor(Field field) {
		boolean fieldIsFinal = Modifier.isFinal(field.getModifiers());
		var fieldType = Type.getType(field.getType());
		String fieldName = field.getName();

		// final fields cannot remain as such, since the ensureMethod will update them
		// and it is not a constructor. Java < 9 will not check this constraint but
		// newer versions of Java would reject the code without this change
		if (fieldIsFinal) {
			org.apache.bcel.classfile.Field oldField = getFields()
				.filter(f -> f.getName().equals(fieldName) && f.getType().equals(fieldType))
				.findFirst().get(); // if this does not exist, there is a bug in the code since field was a non-transient field of the class
			var newField = new FieldGen(oldField, cpg);
			newField.setAccessFlags(oldField.getAccessFlags() ^ Const.ACC_FINAL);
			replaceField(oldField, newField.getField());
		}

		var il = new InstructionList();
		InstructionHandle _return = il.append(InstructionConst.RETURN);
		il.insert(_return, InstructionFactory.createThis());
		// we use reflection to access the private field "inStorage"
		il.insert(_return, factory.createInvoke(WhitelistingConstants.RUNTIME_NAME, "inStorageOf", Type.BOOLEAN, new Type[] { Type.OBJECT }, Const.INVOKESTATIC));
		il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFEQ, _return));
		il.insert(_return, InstructionFactory.createThis());
		il.insert(_return, factory.createGetField(className, InstrumentationConstants.IF_ALREADY_LOADED_PREFIX + fieldName, BasicType.BOOLEAN));
		il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFNE, _return));
		il.insert(_return, InstructionFactory.createThis());
		il.insert(_return, InstructionConst.DUP);
		il.insert(_return, InstructionConst.DUP);
		il.insert(_return, InstructionConst.ICONST_1);
		il.insert(_return, factory.createPutField(className, InstrumentationConstants.IF_ALREADY_LOADED_PREFIX + fieldName, BasicType.BOOLEAN));
		il.insert(_return, factory.createConstant(className));
		il.insert(_return, factory.createConstant(fieldName));
		il.insert(_return, factory.createConstant(field.getType().getName()));
		il.insert(_return, factory.createInvoke(WhitelistingConstants.RUNTIME_NAME,
			fieldIsFinal ? InstrumentationConstants.DESERIALIZE_LAST_UPDATE_FOR_FINAL : InstrumentationConstants.DESERIALIZE_LAST_UPDATE_FOR,
			ObjectType.OBJECT, DESERIALIZE_LAST_UPDATE_ARGS, Const.INVOKESTATIC));
		il.insert(_return, factory.createCast(ObjectType.OBJECT, fieldType));
		il.insert(_return, InstructionConst.DUP2);
		il.insert(_return, factory.createPutField(className, fieldName, fieldType));
		il.insert(_return, factory.createPutField(className, InstrumentationFields.OLD_PREFIX + fieldName, fieldType));

		addMethod(new MethodGen(PRIVATE_SYNTHETIC, BasicType.VOID, Type.NO_ARGS, null, InstrumentationConstants.ENSURE_LOADED_PREFIX + fieldName, className, il, cpg));
	}
}