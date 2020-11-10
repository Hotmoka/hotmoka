package io.takamaka.code.instrumentation.internal.instrumentationsOfMethod;

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

import io.takamaka.code.constants.Constants;
import io.takamaka.code.instrumentation.InstrumentationConstants;
import io.takamaka.code.instrumentation.internal.InstrumentedClassImpl;
import io.takamaka.code.verification.ThrowIncompleteClasspathError;

/**
 * Replaces accesses to fields of storage classes with calls to accessor methods.
 */
public class ReplaceFieldAccessesWithAccessors extends InstrumentedClassImpl.Builder.MethodLevelInstrumentation {

	public ReplaceFieldAccessesWithAccessors(InstrumentedClassImpl.Builder builder, MethodGen method) {
		builder.super(method);

		if (!method.isAbstract()) {
			InstructionList il = method.getInstructionList();
			StreamSupport.stream(il.spliterator(), false).filter(this::isAccessToLazilyLoadedFieldInStorageClass)
				.forEach(ih -> ih.setInstruction(accessorCorrespondingTo((FieldInstruction) ih.getInstruction())));
		}
	}

	/**
	 * Determines if the given instruction is an access to a field of a storage
	 * class that is lazily loaded.
	 * 
	 * @param ih the instruction
	 * @return true if and only if that condition holds
	 */
	private boolean isAccessToLazilyLoadedFieldInStorageClass(InstructionHandle ih) {
		Instruction instruction = ih.getInstruction();

		if (instruction instanceof GETFIELD || instruction instanceof PUTFIELD) {
			FieldInstruction fi = (FieldInstruction) instruction;
			ObjectType receiverType = (ObjectType) fi.getReferenceType(cpg);
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
	 */
	private Instruction accessorCorrespondingTo(FieldInstruction fieldInstruction) {
		ObjectType referencedClass = (ObjectType) fieldInstruction.getReferenceType(cpg);
		Type fieldType = fieldInstruction.getFieldType(cpg);
		String fieldName = fieldInstruction.getFieldName(cpg);
		String className = referencedClass.getClassName();

		if (fieldInstruction instanceof GETFIELD)
			return factory.createInvoke(className, getterNameFor(className, fieldName), fieldType, Type.NO_ARGS, Const.INVOKEVIRTUAL);
		else // PUTFIELD
			return factory.createInvoke(className, setterNameFor(className, fieldName), Type.VOID, new Type[] { fieldType }, Const.INVOKEVIRTUAL);
	}

	/**
	 * Determines if a storage class has a field with modifiers that satisfy the given condition.
	 * 
	 * @param className the class from which the field must be looked-up. This is guaranteed to be a storage class
	 * @param fieldName the name of the field
	 * @param fieldType the type of the field
	 * @param condition the condition on the modifiers of the field
	 * @return true if and only if that condition holds
	 */
	private boolean modifiersSatisfy(String className, String fieldName, Class<?> fieldType, Predicate<Integer> condition) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Class<?> clazz = classLoader.loadClass(className);
			Class<?> previous = null;
			
			do {
				// these two fields are added by instrumentation hence not found by reflection: they are transient
				if (clazz == classLoader.getStorage() &&
						(fieldName.equals(InstrumentationConstants.STORAGE_REFERENCE_FIELD_NAME) || fieldName.equals(InstrumentationConstants.IN_STORAGE)))
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
		});
	}
}