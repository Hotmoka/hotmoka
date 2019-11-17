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

import io.takamaka.code.instrumentation.internal.ClassInstrumentation;

/**
 * An instrumentation that adds accessor methods for the fields of the class being instrumented.
 */
public class AddAccessorMethods {

	public AddAccessorMethods(ClassInstrumentation.Instrumenter instrumenter) {
		instrumenter.lazyNonTransientInstanceFields.forEach(field -> addAccessorMethodsFor(field, instrumenter));
	}

	/**
	 * Adds accessor methods for the given field.
	 * 
	 * @param field the field
	 */
	private void addAccessorMethodsFor(Field field, ClassInstrumentation.Instrumenter instrumenter) {
		addGetterFor(field, instrumenter);

		if (!Modifier.isFinal(field.getModifiers()))
			addSetterFor(field, instrumenter);
	}

	/**
	 * Adds a setter method for the given field.
	 * 
	 * @param field the field
	 */
	private void addSetterFor(Field field, ClassInstrumentation.Instrumenter instrumenter) {
		Type type = Type.getType(field.getType());
		InstructionList il = new InstructionList();
		il.append(InstructionFactory.createThis());
		il.append(InstructionConst.DUP);
		il.append(instrumenter.factory.createInvoke(instrumenter.className, ClassInstrumentation.ENSURE_LOADED_PREFIX + field.getName(),
			BasicType.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
		il.append(InstructionConst.ALOAD_1);
		il.append(instrumenter.factory.createPutField(instrumenter.className, field.getName(), type));
		il.append(InstructionConst.RETURN);

		MethodGen setter = new MethodGen(ClassInstrumentation.PUBLIC_SYNTHETIC_FINAL, BasicType.VOID, new Type[] { type }, null,
			instrumenter.setterNameFor(instrumenter.className, field.getName()), instrumenter.className, il, instrumenter.cpg);
		instrumenter.addMethod(setter, false);
	}

	/**
	 * Adds a getter method for the given field.
	 * 
	 * @param field the field
	 */
	private void addGetterFor(Field field, ClassInstrumentation.Instrumenter instrumenter) {
		Type type = Type.getType(field.getType());
		InstructionList il = new InstructionList();
		il.append(InstructionFactory.createThis());
		il.append(InstructionConst.DUP);
		il.append(instrumenter.factory.createInvoke(instrumenter.className, ClassInstrumentation.ENSURE_LOADED_PREFIX + field.getName(),
			BasicType.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
		il.append(instrumenter.factory.createGetField(instrumenter.className, field.getName(), type));
		il.append(InstructionFactory.createReturn(type));

		MethodGen getter = new MethodGen(ClassInstrumentation.PUBLIC_SYNTHETIC_FINAL, type, Type.NO_ARGS, null,
			instrumenter.getterNameFor(instrumenter.className, field.getName()), instrumenter.className, il, instrumenter.cpg);
		instrumenter.addMethod(getter, false);
	}
}