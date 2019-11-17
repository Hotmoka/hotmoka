package io.takamaka.code.instrumentation.internal.instrumentationsOfClass;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.instrumentation.internal.ClassInstrumentation;
import io.takamaka.code.verification.Constants;

/**
 * An instrumentation that adds, to a storage class, the method that extract all updates to an instance
 * of a class, since the beginning of a transaction.
 */
public class AddExtractUpdates {

	private final static ObjectType LIST_OT = new ObjectType(List.class.getName());
	private final static ObjectType ENUM_OT = new ObjectType(Enum.class.getName());
	private final static ObjectType SET_OT = new ObjectType(Set.class.getName());
	private final static Type[] EXTRACT_UPDATES_ARGS = { SET_OT, SET_OT, LIST_OT };
	private final static Type[] ADD_UPDATES_FOR_ARGS = { Type.STRING, Type.STRING, SET_OT };
	private final static Type[] RECURSIVE_EXTRACT_ARGS = { Type.OBJECT, SET_OT, SET_OT, LIST_OT };
	private final static Type[] TWO_OBJECTS_ARGS = { Type.OBJECT, Type.OBJECT };

	public AddExtractUpdates(ClassInstrumentation.Instrumenter instrumenter) {
		if (!instrumenter.eagerNonTransientInstanceFields.getLast().isEmpty() || !instrumenter.lazyNonTransientInstanceFields.isEmpty()) {
			InstructionList il = new InstructionList();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.DUP);
			il.append(InstructionConst.ALOAD_1);
			il.append(InstructionConst.ALOAD_2);
			il.append(InstructionFactory.createLoad(LIST_OT, 3));
			il.append(instrumenter.factory.createInvoke(instrumenter.clazz.getSuperclassName(), ClassInstrumentation.EXTRACT_UPDATES, Type.VOID,
					EXTRACT_UPDATES_ARGS, Const.INVOKESPECIAL));
			il.append(instrumenter.factory.createGetField(Constants.ABSTRACT_STORAGE_NAME, ClassInstrumentation.IN_STORAGE_NAME, Type.BOOLEAN));
			il.append(InstructionFactory.createStore(Type.BOOLEAN, 4));

			InstructionHandle end = il.append(InstructionConst.RETURN);

			for (Field field: instrumenter.eagerNonTransientInstanceFields.getLast())
				end = addUpdateExtractionForEagerField(field, il, end, instrumenter);

			for (Field field: instrumenter.lazyNonTransientInstanceFields)
				end = addUpdateExtractionForLazyField(field, il, end, instrumenter);

			MethodGen extractUpdates = new MethodGen(ClassInstrumentation.PROTECTED_SYNTHETIC, Type.VOID, EXTRACT_UPDATES_ARGS, null,
				ClassInstrumentation.EXTRACT_UPDATES, instrumenter.className, il, instrumenter.cpg);
			instrumenter.addMethod(extractUpdates, true);
		}
	}

	/**
	 * Adds the code that check if a given lazy field has been updated since the
	 * beginning of a transaction and, in such a case, adds the corresponding update.
	 * 
	 * @param field the field
	 * @param il    the instruction list where the code must be added
	 * @param end   the instruction before which the extra code must be added
	 * @return the beginning of the added code
	 */
	private InstructionHandle addUpdateExtractionForLazyField(Field field, InstructionList il, InstructionHandle end, ClassInstrumentation.Instrumenter instrumenter) {
		Type type = Type.getType(field.getType());
		InstructionFactory factory = instrumenter.factory;

		List<Type> args = new ArrayList<>();
		for (Type arg: ADD_UPDATES_FOR_ARGS)
			args.add(arg);
		args.add(SET_OT);
		args.add(LIST_OT);
		args.add(ObjectType.STRING);
		args.add(ObjectType.OBJECT);

		InstructionHandle recursiveExtract;
		// we deal with special cases where the call to a recursive extract is useless:
		// this is just an optimization
		String fieldName = field.getName();
		if (field.getType() == String.class || field.getType() == BigInteger.class)
			recursiveExtract = end;
		else {
			recursiveExtract = il.insert(end, InstructionFactory.createThis());
			il.insert(end, InstructionConst.DUP);
			il.insert(end, factory.createGetField(instrumenter.className, ClassInstrumentation.OLD_PREFIX + fieldName, type));
			il.insert(end, InstructionConst.ALOAD_1);
			il.insert(end, InstructionConst.ALOAD_2);
			il.insert(end, InstructionFactory.createLoad(LIST_OT, 3));
			il.insert(end, factory.createInvoke(Constants.ABSTRACT_STORAGE_NAME, ClassInstrumentation.RECURSIVE_EXTRACT, Type.VOID,
				RECURSIVE_EXTRACT_ARGS, Const.INVOKESPECIAL));
		}

		InstructionHandle addUpdatesFor = il.insert(recursiveExtract, InstructionFactory.createThis());
		il.insert(recursiveExtract, factory.createConstant(instrumenter.className));
		il.insert(recursiveExtract, factory.createConstant(fieldName));
		il.insert(recursiveExtract, InstructionConst.ALOAD_1);
		il.insert(recursiveExtract, InstructionConst.ALOAD_2);
		il.insert(recursiveExtract, InstructionFactory.createLoad(LIST_OT, 3));
		il.insert(recursiveExtract, factory.createConstant(field.getType().getName()));
		il.insert(recursiveExtract, InstructionFactory.createThis());
		il.insert(recursiveExtract, factory.createGetField(instrumenter.className, fieldName, type));
		il.insert(recursiveExtract, factory.createInvoke(Constants.ABSTRACT_STORAGE_NAME, ClassInstrumentation.ADD_UPDATE_FOR, Type.VOID,
				args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

		InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createLoad(Type.BOOLEAN, 4));
		il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
		il.insert(addUpdatesFor, InstructionFactory.createThis());
		il.insert(addUpdatesFor, factory.createGetField(instrumenter.className, fieldName, type));
		il.insert(addUpdatesFor, InstructionFactory.createThis());
		il.insert(addUpdatesFor, factory.createGetField(instrumenter.className, ClassInstrumentation.OLD_PREFIX + fieldName, type));

		il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IF_ACMPEQ, recursiveExtract));

		return start;
	}

	/**
	 * Adds the code that check if a given eager field has been updated since the
	 * beginning of a transaction and, in such a case, adds the corresponding
	 * update.
	 * 
	 * @param field the field
	 * @param il    the instruction list where the code must be added
	 * @param end   the instruction before which the extra code must be added
	 * @return the beginning of the added code
	 */
	private InstructionHandle addUpdateExtractionForEagerField(Field field, InstructionList il, InstructionHandle end, ClassInstrumentation.Instrumenter instrumenter) {
		InstructionFactory factory = instrumenter.factory;
		Class<?> fieldType = field.getType();
		Type type = Type.getType(fieldType);
		boolean isEnum = fieldType.isEnum();

		List<Type> args = new ArrayList<>();
		for (Type arg: ADD_UPDATES_FOR_ARGS)
			args.add(arg);
		if (isEnum) {
			args.add(ObjectType.STRING);
			args.add(ENUM_OT);
		}
		else
			args.add(type);

		InstructionHandle addUpdatesFor = il.insert(end, InstructionFactory.createThis());
		il.insert(end, factory.createConstant(instrumenter.className));
		il.insert(end, factory.createConstant(field.getName()));
		il.insert(end, InstructionConst.ALOAD_1);
		if (isEnum)
			il.insert(end, factory.createConstant(fieldType.getName()));
		il.insert(end, InstructionFactory.createThis());
		il.insert(end, factory.createGetField(instrumenter.className, field.getName(), type));
		il.insert(end, factory.createInvoke(Constants.ABSTRACT_STORAGE_NAME, ClassInstrumentation.ADD_UPDATE_FOR, Type.VOID,
				args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

		InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createLoad(Type.BOOLEAN, 4));
		il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
		il.insert(addUpdatesFor, InstructionFactory.createThis());
		il.insert(addUpdatesFor, factory.createGetField(instrumenter.className, field.getName(), type));
		il.insert(addUpdatesFor, InstructionFactory.createThis());
		il.insert(addUpdatesFor, factory.createGetField(instrumenter.className, ClassInstrumentation.OLD_PREFIX + field.getName(), type));

		if (fieldType == double.class) {
			il.insert(addUpdatesFor, InstructionConst.DCMPL);
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
		}
		else if (fieldType == float.class) {
			il.insert(addUpdatesFor, InstructionConst.FCMPL);
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
		}
		else if (fieldType == long.class) {
			il.insert(addUpdatesFor, InstructionConst.LCMP);
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
		}
		else if (fieldType == String.class || fieldType == BigInteger.class) {
			// comparing strings or BigInteger with their previous value is done by checking if they
			// are equals rather than ==. This is just an optimization, to avoid storing an equivalent value
			// as an update. It is relevant for the balance fields of contracts, that might reach 0 at the
			// end of a transaction, as it was at the beginning, but has fluctuated during the
			// transaction: it is useless to add an update for it
			il.insert(addUpdatesFor, factory.createInvoke("java.util.Objects", "equals", Type.BOOLEAN,
				TWO_OBJECTS_ARGS, Const.INVOKESTATIC));
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFNE, end));
		}
		else if (!fieldType.isPrimitive())
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IF_ACMPEQ, end));
		else
			// this covers int, short, byte, char, boolean
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IF_ICMPEQ, end));

		return start;
	}
}