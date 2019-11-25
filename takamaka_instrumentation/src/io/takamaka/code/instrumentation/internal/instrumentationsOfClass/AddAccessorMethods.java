package io.takamaka.code.instrumentation.internal.instrumentationsOfClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import io.takamaka.code.instrumentation.internal.InstrumentedClassImpl;

/**
 * An instrumentation that adds accessor methods for the fields of the class being instrumented.
 */
public class AddAccessorMethods extends InstrumentedClassImpl.Builder.ClassLevelInstrumentation {
	private final static short PUBLIC_SYNTHETIC_FINAL = Const.ACC_PUBLIC | Const.ACC_SYNTHETIC | Const.ACC_FINAL;

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
		Type type = Type.getType(field.getType());
		InstructionList il = new InstructionList();
		il.append(InstructionFactory.createThis());
		il.append(InstructionConst.DUP);
		il.append(factory.createInvoke(className, InstrumentedClassImpl.ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
		il.append(InstructionConst.ALOAD_1);
		il.append(factory.createPutField(className, field.getName(), type));
		il.append(InstructionConst.RETURN);

		MethodGen setter = new MethodGen(PUBLIC_SYNTHETIC_FINAL, BasicType.VOID, new Type[] { type }, null,
			setterNameFor(className, field.getName()), className, il, cpg);
		addMethod(setter, false);
	}

	/**
	 * Adds a getter method for the given field.
	 * 
	 * @param field the field
	 */
	private void addGetterFor(Field field) {
		Type type = Type.getType(field.getType());
		InstructionList il = new InstructionList();
		il.append(InstructionFactory.createThis());
		il.append(InstructionConst.DUP);
		il.append(factory.createInvoke(className, InstrumentedClassImpl.ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
		il.append(factory.createGetField(className, field.getName(), type));
		il.append(InstructionFactory.createReturn(type));

		MethodGen getter = new MethodGen(PUBLIC_SYNTHETIC_FINAL, type, Type.NO_ARGS, null,
			getterNameFor(className, field.getName()), className, il, cpg);
		addMethod(getter, false);
	}
}