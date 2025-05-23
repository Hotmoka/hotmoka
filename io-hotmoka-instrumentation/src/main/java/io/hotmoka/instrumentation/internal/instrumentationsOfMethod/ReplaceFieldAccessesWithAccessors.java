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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.Type;

import io.hotmoka.instrumentation.api.InstrumentationFields;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.MethodLevelInstrumentation;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.UnknownTypeException;
import io.takamaka.code.constants.Constants;

/**
 * Replaces accesses to fields of storage classes with calls to their accessor method.
 */
public class ReplaceFieldAccessesWithAccessors extends MethodLevelInstrumentation {

	/**
	 * Builds the instrumentation.
	 * 
	 * @param builder the builder of the class being instrumented
	 * @param method the method being instrumented
	 * @throws IllegalJarException if the jar under instrumentation is illegal
	 * @throws UnknownTypeException if some type cannot be resolved
	 * @throws ClassNotFoundException 
	 */
	public ReplaceFieldAccessesWithAccessors(InstrumentedClassImpl.Builder builder, MethodGen method) throws IllegalJarException, UnknownTypeException {
		builder.super(method);

		var il = method.getInstructionList();
		if (il != null)
			for (var ih: il) {
				var maybeFieldInstruction = getFieldInstructionAccessingLazilyLoadedFieldInStorageClass(ih);
				if (maybeFieldInstruction.isPresent())
					ih.setInstruction(accessorCorrespondingTo(maybeFieldInstruction.get()));
			}
	}

	/**
	 * Determines if the given handle holds an instruction that accesses a field of a storage class that is lazily loaded.
	 * 
	 * @param ih the instruction handle
	 * @return the instruction in {@code ih}, if it accesses a lazily loaded field of a storage class
	 * @throws IllegalJarException if the jar under instrumentation is illegal
	 * @throws UnknownTypeException if some type cannot be resolved
	 * @throws ClassNotFoundException 
	 */
	private Optional<FieldInstruction> getFieldInstructionAccessingLazilyLoadedFieldInStorageClass(InstructionHandle ih) throws IllegalJarException, UnknownTypeException {
		var instruction = ih.getInstruction();

		if (instruction instanceof FieldInstruction fi && (instruction instanceof GETFIELD || instruction instanceof PUTFIELD)) {
			if (!(fi.getReferenceType(cpg) instanceof ObjectType receiverType))
				throw new IllegalJarException("Attempt to read a field of a non-object reference");

			String receiverClassName = receiverType.getClassName();
			Class<?> fieldType;

			// we do not consider field accesses added by instrumentation in class Storage

			try {
				if (!Constants.STORAGE_NAME.equals(receiverClassName)
						&& classLoader.isStorage(receiverClassName)
						&& classLoader.isLazilyLoaded(fieldType = bcelToClass.of(fi.getFieldType(cpg)))
						&& !modifiersSatisfy(receiverClassName, fi.getFieldName(cpg), fieldType,
								instruction instanceof GETFIELD ? Modifier::isTransient : (modifiers -> Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers))))
					return Optional.of(fi);
			}
			catch (ClassNotFoundException e) {
				throw new UnknownTypeException(receiverClassName);
			}
		}

		return Optional.empty();
	}

	/**
	 * Yields the accessor call corresponding to the access to the given field.
	 * 
	 * @param fieldInstruction the field access instruction
	 * @return the corresponding accessor call instruction
	 * @throws IllegalJarException if the jar under instrumentation is illegal
	 * @throws UnknownTypeException if some type cannot be resolved
	 */
	private Instruction accessorCorrespondingTo(FieldInstruction fieldInstruction) throws UnknownTypeException {
		ObjectType referencedClass = (ObjectType) fieldInstruction.getReferenceType(cpg); // cast already checked above
		Type fieldType = fieldInstruction.getFieldType(cpg);
		String fieldName = fieldInstruction.getFieldName(cpg);
		String className = referencedClass.getClassName();
		Field resolvedField;

		try {
			resolvedField = classLoader.resolveField(className, fieldName, bcelToClass.of(fieldType)).get();
		}
		catch (ClassNotFoundException e) {
			throw new UnknownTypeException(className);
		}

		String resolvedClassName = resolvedField.getDeclaringClass().getName();

		if (fieldInstruction instanceof GETFIELD)
			return factory.createInvoke(className, getterNameFor(resolvedClassName, fieldName), fieldType, Type.NO_ARGS, Const.INVOKEVIRTUAL);
		else // we know this is a PUTFIELD, for the way this method is called
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
	 * @throws IllegalJarException if the jar under instrumentation is illegal
	 * @throws UnknownTypeException if some type cannot be resolved
	 */
	private boolean modifiersSatisfy(String className, String fieldName, Class<?> fieldType, Predicate<Integer> condition) throws IllegalJarException, UnknownTypeException {
		Class<?> clazz, previous;
		
		try {
			clazz = classLoader.loadClass(className);
		}
		catch (ClassNotFoundException e) {
			throw new UnknownTypeException(className);
		}

		Class<?> storage = classLoader.getStorage();

		do {
			// these two fields are added by instrumentation hence not found by reflection: they are transient, the first is final, the second is not final
			if (clazz == storage)
				if (InstrumentationFields.STORAGE_REFERENCE_FIELD_NAME.equals(fieldName))
					return condition.test(Modifier.TRANSIENT | Modifier.FINAL);
				else if (InstrumentationFields.IN_STORAGE.equals(fieldName))
					return condition.test(Modifier.TRANSIENT);

			Optional<Field> match = Stream.of(clazz.getDeclaredFields())
					.filter(field -> field.getName().equals(fieldName) && fieldType == field.getType())
					.findFirst();

			if (match.isPresent())
				return condition.test(match.get().getModifiers());

			previous = clazz;
			clazz = clazz.getSuperclass();
		}
		while (previous != storage);

		// the field was white-listed, hence resolvable, therefore this should never happen
		throw new RuntimeException("Field " + className + "." + fieldName + " not found");
	}
}