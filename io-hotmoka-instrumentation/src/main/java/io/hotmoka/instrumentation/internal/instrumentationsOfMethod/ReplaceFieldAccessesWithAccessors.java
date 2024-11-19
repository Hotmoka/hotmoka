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

package io.hotmoka.instrumentation.internal.instrumentationsOfMethod;

import static io.hotmoka.exceptions.CheckRunnable.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.Const;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.Type;

import io.hotmoka.exceptions.UncheckConsumer;
import io.hotmoka.instrumentation.api.InstrumentationFields;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.MethodLevelInstrumentation;
import io.takamaka.code.constants.Constants;

/**
 * Replaces accesses to fields of storage classes with calls to accessor methods.
 */
public class ReplaceFieldAccessesWithAccessors extends MethodLevelInstrumentation {

	/**
	 * Builds the instrumentation.
	 * 
	 * @param builder the builder of the class being instrumented
	 * @param method the method being instrumented
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	public ReplaceFieldAccessesWithAccessors(InstrumentedClassImpl.Builder builder, MethodGen method) throws ClassNotFoundException {
		builder.super(method);

		if (!method.isAbstract()) {
			InstructionList il = method.getInstructionList();
			check(ClassNotFoundException.class, () ->
				StreamSupport.stream(il.spliterator(), false).filter(uncheck(ClassNotFoundException.class, this::isAccessToLazilyLoadedFieldInStorageClass))
					.forEach(UncheckConsumer.uncheck(ClassNotFoundException.class, ih -> ih.setInstruction(accessorCorrespondingTo((FieldInstruction) ih.getInstruction()))))
			);
		}
	}

	/**
	 * Determines if the given instruction is an access to a field of a storage
	 * class that is lazily loaded.
	 * 
	 * @param ih the instruction
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if the storage class cannot be found in the Takamaka program
	 */
	private boolean isAccessToLazilyLoadedFieldInStorageClass(InstructionHandle ih) throws ClassNotFoundException {
		Instruction instruction = ih.getInstruction();

		if (instruction instanceof GETFIELD || instruction instanceof PUTFIELD) {
			var fi = (FieldInstruction) instruction;
			var receiverType = (ObjectType) fi.getReferenceType(cpg);
			String receiverClassName = receiverType.getClassName();
			Class<?> fieldType;
			// we do not consider field accesses added by instrumentation in class Storage
			return !receiverClassName.equals(Constants.STORAGE_NAME)
				&& classLoader.isStorage(receiverClassName)
				&& classLoader.isLazilyLoaded(fieldType = verifiedClass.getJar().getBcelToClass().of(fi.getFieldType(cpg)))
				&& !modifiersSatisfy(receiverClassName, fi.getFieldName(cpg), fieldType,
						instruction instanceof GETFIELD ? Modifier::isTransient : (modifiers -> Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers)));
		}
		else
			return false;
	}

	/**
	 * Yields the accessor call corresponding to the access to the given field.
	 * 
	 * @param fieldInstruction the field access instruction
	 * @return the corresponding accessor call instruction
	 * @throws ClassNotFoundException 
	 */
	private Instruction accessorCorrespondingTo(FieldInstruction fieldInstruction) throws ClassNotFoundException {
		ObjectType referencedClass = (ObjectType) fieldInstruction.getReferenceType(cpg);
		Type fieldType = fieldInstruction.getFieldType(cpg);
		String fieldName = fieldInstruction.getFieldName(cpg);
		String className = referencedClass.getClassName();
		Optional<Field> resolvedField = classLoader.resolveField(className, fieldName, verifiedClass.getJar().getBcelToClass().of(fieldType));
		String resolvedClassName = resolvedField.get().getDeclaringClass().getName();

		if (fieldInstruction instanceof GETFIELD)
			return factory.createInvoke(className, getterNameFor(resolvedClassName, fieldName), fieldType, Type.NO_ARGS, Const.INVOKEVIRTUAL);
		else // PUTFIELD
			return factory.createInvoke(className, setterNameFor(resolvedClassName, fieldName), Type.VOID, new Type[] { fieldType }, Const.INVOKEVIRTUAL);
	}

	/**
	 * Determines if a storage class has a field with modifiers that satisfy the given condition.
	 * 
	 * @param className the class from which the field must be looked-up. This is guaranteed to be a storage class
	 * @param fieldName the name of the field
	 * @param fieldType the type of the field
	 * @param condition the condition on the modifiers of the field
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code cassName} cannot be found in the Takamaka program
	 */
	private boolean modifiersSatisfy(String className, String fieldName, Class<?> fieldType, Predicate<Integer> condition) throws ClassNotFoundException {
		Class<?> clazz = classLoader.loadClass(className);
		Class<?> previous;

		do {
			// these two fields are added by instrumentation hence not found by reflection: they are transient
			if (clazz == classLoader.getStorage() &&
					(fieldName.equals(InstrumentationFields.STORAGE_REFERENCE_FIELD_NAME) || fieldName.equals(InstrumentationFields.IN_STORAGE)))
				return true;

			Optional<Field> match = Stream.of(clazz.getDeclaredFields())
					.filter(field -> field.getName().equals(fieldName) && fieldType == field.getType())
					.findFirst();

			if (match.isPresent())
				return condition.test(match.get().getModifiers());

			previous = clazz;
			clazz = clazz.getSuperclass();
		}
		while (previous != classLoader.getStorage());

		return false;
	}
}