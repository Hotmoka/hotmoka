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

import io.takamaka.code.instrumentation.internal.InstrumentedClassImpl;
import io.takamaka.code.verification.Constants;

/**
 * An instrumentation that adds, to a storage class, the method that extract all updates to an instance
 * of a class, since the beginning of a transaction.
 */
public class AddExtractUpdates extends InstrumentedClassImpl.Builder.ClassLevelInstrumentation {
	private final static String EXTRACT_UPDATES = "extractUpdates";
	private final static String RECURSIVE_EXTRACT = "recursiveExtract";
	private final static String ADD_UPDATE_FOR = "addUpdateFor";
	private final static ObjectType LIST_OT = new ObjectType(List.class.getName());
	private final static ObjectType ENUM_OT = new ObjectType(Enum.class.getName());
	private final static ObjectType SET_OT = new ObjectType(Set.class.getName());
	private final static Type[] EXTRACT_UPDATES_ARGS = { SET_OT, SET_OT, LIST_OT };
	private final static Type[] ADD_UPDATES_FOR_ARGS = { Type.STRING, Type.STRING, SET_OT };
	private final static Type[] RECURSIVE_EXTRACT_ARGS = { Type.OBJECT, SET_OT, SET_OT, LIST_OT };
	private final static Type[] TWO_OBJECTS_ARGS = { Type.OBJECT, Type.OBJECT };
	private final static short PROTECTED_SYNTHETIC = Const.ACC_PROTECTED | Const.ACC_SYNTHETIC;

	public AddExtractUpdates(InstrumentedClassImpl.Builder builder) {
		builder.super();

		if (isStorage && (!eagerNonTransientInstanceFields.getLast().isEmpty() || !lazyNonTransientInstanceFields.isEmpty())) {
			InstructionList il = new InstructionList();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.DUP);
			il.append(InstructionConst.ALOAD_1);
			il.append(InstructionConst.ALOAD_2);
			il.append(InstructionFactory.createLoad(LIST_OT, 3));
			il.append(factory.createInvoke(getSuperclassName(), EXTRACT_UPDATES, Type.VOID,
					EXTRACT_UPDATES_ARGS, Const.INVOKESPECIAL));
			il.append(factory.createGetField(Constants.ABSTRACT_STORAGE_NAME, InstrumentedClassImpl.IN_STORAGE_NAME, Type.BOOLEAN));
			il.append(InstructionFactory.createStore(Type.BOOLEAN, 4));

			InstructionHandle end = il.append(InstructionConst.RETURN);

			for (Field field: eagerNonTransientInstanceFields.getLast())
				end = addUpdateExtractionForEagerField(field, il, end);

			for (Field field: lazyNonTransientInstanceFields)
				end = addUpdateExtractionForLazyField(field, il, end);

			MethodGen extractUpdates = new MethodGen(PROTECTED_SYNTHETIC, Type.VOID, EXTRACT_UPDATES_ARGS, null, EXTRACT_UPDATES, className, il, cpg);
			addMethod(extractUpdates, true);
		}
	}

	/**
	 * Adds the code that check if a given lazy field has been updated since the
	 * beginning of a transaction and, in such a case, adds the corresponding update.
	 * 
	 * @param field the field
	 * @param il the instruction list where the code must be added
	 * @param end the instruction before which the extra code must be added
	 * @return the beginning of the added code
	 */
	private InstructionHandle addUpdateExtractionForLazyField(Field field, InstructionList il, InstructionHandle end) {
		Type type = Type.getType(field.getType());

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
			il.insert(end, factory.createGetField(className, InstrumentedClassImpl.OLD_PREFIX + fieldName, type));
			il.insert(end, InstructionConst.ALOAD_1);
			il.insert(end, InstructionConst.ALOAD_2);
			il.insert(end, InstructionFactory.createLoad(LIST_OT, 3));
			il.insert(end, factory.createInvoke(Constants.ABSTRACT_STORAGE_NAME, RECURSIVE_EXTRACT, Type.VOID,
				RECURSIVE_EXTRACT_ARGS, Const.INVOKESPECIAL));
		}

		InstructionHandle addUpdatesFor = il.insert(recursiveExtract, InstructionFactory.createThis());
		il.insert(recursiveExtract, factory.createConstant(className));
		il.insert(recursiveExtract, factory.createConstant(fieldName));
		il.insert(recursiveExtract, InstructionConst.ALOAD_1);
		il.insert(recursiveExtract, InstructionConst.ALOAD_2);
		il.insert(recursiveExtract, InstructionFactory.createLoad(LIST_OT, 3));
		il.insert(recursiveExtract, factory.createConstant(field.getType().getName()));
		il.insert(recursiveExtract, InstructionFactory.createThis());
		il.insert(recursiveExtract, factory.createGetField(className, fieldName, type));
		il.insert(recursiveExtract, factory.createInvoke(Constants.ABSTRACT_STORAGE_NAME, ADD_UPDATE_FOR, Type.VOID,
				args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

		InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createLoad(Type.BOOLEAN, 4));
		il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
		il.insert(addUpdatesFor, InstructionFactory.createThis());
		il.insert(addUpdatesFor, factory.createGetField(className, fieldName, type));
		il.insert(addUpdatesFor, InstructionFactory.createThis());
		il.insert(addUpdatesFor, factory.createGetField(className, InstrumentedClassImpl.OLD_PREFIX + fieldName, type));

		il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IF_ACMPEQ, recursiveExtract));

		return start;
	}

	/**
	 * Adds the code that check if a given eager field has been updated since the
	 * beginning of a transaction and, in such a case, adds the corresponding update.
	 * 
	 * @param field the field
	 * @param il the instruction list where the code must be added
	 * @param end the instruction before which the extra code must be added
	 * @return the beginning of the added code
	 */
	private InstructionHandle addUpdateExtractionForEagerField(Field field, InstructionList il, InstructionHandle end) {
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
		il.insert(end, factory.createConstant(className));
		il.insert(end, factory.createConstant(field.getName()));
		il.insert(end, InstructionConst.ALOAD_1);
		if (isEnum)
			il.insert(end, factory.createConstant(fieldType.getName()));
		il.insert(end, InstructionFactory.createThis());
		il.insert(end, factory.createGetField(className, field.getName(), type));
		il.insert(end, factory.createInvoke(Constants.ABSTRACT_STORAGE_NAME, ADD_UPDATE_FOR, Type.VOID, args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

		InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createLoad(Type.BOOLEAN, 4));
		il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
		il.insert(addUpdatesFor, InstructionFactory.createThis());
		il.insert(addUpdatesFor, factory.createGetField(className, field.getName(), type));
		il.insert(addUpdatesFor, InstructionFactory.createThis());
		il.insert(addUpdatesFor, factory.createGetField(className, InstrumentedClassImpl.OLD_PREFIX + field.getName(), type));

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