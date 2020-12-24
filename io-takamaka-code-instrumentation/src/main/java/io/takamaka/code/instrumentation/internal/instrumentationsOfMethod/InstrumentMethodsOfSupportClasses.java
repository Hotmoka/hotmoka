package io.takamaka.code.instrumentation.internal.instrumentationsOfMethod;

import java.math.BigInteger;
import java.util.concurrent.Callable;

import org.apache.bcel.Const;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.constants.Constants;
import io.takamaka.code.instrumentation.InstrumentationConstants;
import io.takamaka.code.instrumentation.internal.InstrumentedClassImpl;

/**
 * Edits the code of the methods in some support classes of Takamaka.
 */
public class InstrumentMethodsOfSupportClasses extends InstrumentedClassImpl.Builder.MethodLevelInstrumentation {
	private final static ObjectType STORAGE_OT = new ObjectType(Constants.STORAGE_NAME);
	private final static ObjectType EVENT_OT = new ObjectType(Constants.EVENT_NAME);
	private final static ObjectType BIGINTEGER_OT = new ObjectType(BigInteger.class.getName());

	public InstrumentMethodsOfSupportClasses(InstrumentedClassImpl.Builder builder, MethodGen method) {
		builder.super(method);

		Type[] args;

		if (className.equals(Constants.STORAGE_NAME)) {
			if ("compareByStorageReference".equals(method.getName()) && (args = method.getArgumentTypes()).length == 1 && STORAGE_OT.equals(args[0])) {
				InstructionList il = new InstructionList();
				il.append(InstructionConst.ALOAD_0);
				il.append(InstructionConst.ALOAD_1);
				il.append(factory.createInvoke(InstrumentationConstants.RUNTIME_NAME, "compareStorageReferencesOf", Type.INT, new Type[] { Type.OBJECT, Type.OBJECT }, Const.INVOKESTATIC));
				il.append(InstructionConst.IRETURN);
				method.setInstructionList(il);
			}
			else if (Const.CONSTRUCTOR_NAME.equals(method.getName()) && method.getArgumentTypes().length == 0) {
				InstructionList il = new InstructionList();
				il.append(InstructionFactory.createThis());
				il.append(factory.createInvoke(Object.class.getName(), Const.CONSTRUCTOR_NAME, Type.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
				il.append(InstructionFactory.createThis());
				il.append(factory.createConstant(false));
				il.append(factory.createPutField(Constants.STORAGE_NAME, InstrumentationConstants.IN_STORAGE, Type.BOOLEAN));
				il.append(InstructionFactory.createThis());
				il.append(factory.createInvoke(InstrumentationConstants.RUNTIME_NAME, "getNextStorageReference", Type.OBJECT, Type.NO_ARGS, Const.INVOKESTATIC));
				il.append(factory.createPutField(Constants.STORAGE_NAME, InstrumentationConstants.STORAGE_REFERENCE_FIELD_NAME, Type.OBJECT));
				il.append(InstructionConst.RETURN);
				method.setInstructionList(il);
			}
		}
		else if (className.equals(Constants.TAKAMAKA_NAME)) {
			if ("event".equals(method.getName()) && (args = method.getArgumentTypes()).length == 1 && EVENT_OT.equals(args[0])) {
				InstructionList il = new InstructionList();
				il.append(InstructionConst.ALOAD_0);
				il.append(factory.createInvoke(InstrumentationConstants.RUNTIME_NAME, "event", Type.VOID, new Type[] { ObjectType.OBJECT }, Const.INVOKESTATIC));
				il.append(InstructionConst.RETURN);
				method.setInstructionList(il);
			}
			else if ("withGas".equals(method.getName()) && (args = method.getArgumentTypes()).length == 2 && BIGINTEGER_OT.equals(args[0])
					&& new ObjectType(Callable.class.getName()).equals(args[1])) {
				InstructionList il = new InstructionList();
				il.append(InstructionConst.ALOAD_0);
				il.append(InstructionConst.ALOAD_1);
				il.append(factory.createInvoke(InstrumentationConstants.RUNTIME_NAME, "withGas", Type.OBJECT, args, Const.INVOKESTATIC));
				il.append(InstructionConst.ARETURN);
				method.setInstructionList(il);
			}
			else if ("now".equals(method.getName()) && method.getArgumentTypes().length == 0) {
				InstructionList il = new InstructionList();
				il.append(factory.createInvoke(InstrumentationConstants.RUNTIME_NAME, "now", Type.LONG, Type.NO_ARGS, Const.INVOKESTATIC));
				il.append(InstructionConst.LRETURN);
				method.setInstructionList(il);
			}
			else if ("isSystemCall".equals(method.getName()) && method.getArgumentTypes().length == 0) {
				InstructionList il = new InstructionList();
				il.append(factory.createInvoke(InstrumentationConstants.RUNTIME_NAME, "isSystemCall", Type.BOOLEAN, Type.NO_ARGS, Const.INVOKESTATIC));
				il.append(InstructionConst.IRETURN);
				method.setInstructionList(il);
			}
		}
	}
}