package io.takamaka.code.instrumentation.internal.instrumentationsOfClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

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

import io.takamaka.code.instrumentation.internal.ClassInstrumentation;
import io.takamaka.code.verification.Constants;

/**
 * An instrumentation that adds the ensure loaded methods for the lazy fields of the class being instrumented.
 */
public class AddEnsureLoadedMethods extends ClassInstrumentation.Builder.ClassLevelInstrumentation {
	private final static Type[] THREE_STRINGS_ARGS = { Type.STRING, Type.STRING, Type.STRING };

	public AddEnsureLoadedMethods(ClassInstrumentation.Builder builder) {
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

		// final fields cannot remain as such, since the ensureMethod will update them
		// and it is not a constructor. Java < 9 will not check this constraint but
		// newer versions of Java would reject the code without this change
		if (fieldIsFinal) {
			org.apache.bcel.classfile.Field oldField = Stream.of(clazz.getFields())
				.filter(f -> f.getName().equals(field.getName()) && f.getType().equals(Type.getType(field.getType())))
				.findFirst().get();
			FieldGen newField = new FieldGen(oldField, cpg);
			newField.setAccessFlags(oldField.getAccessFlags() ^ Const.ACC_FINAL);
			clazz.replaceField(oldField, newField.getField());
		}

		Type type = Type.getType(field.getType());
		InstructionList il = new InstructionList();
		InstructionHandle _return = il.append(InstructionConst.RETURN);
		il.insert(_return, InstructionFactory.createThis());
		il.insert(_return, factory.createGetField(Constants.ABSTRACT_STORAGE_NAME, ClassInstrumentation.IN_STORAGE_NAME, BasicType.BOOLEAN));
		il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFEQ, _return));
		il.insert(_return, InstructionFactory.createThis());
		String fieldName = field.getName();
		il.insert(_return, factory.createGetField(className, ClassInstrumentation.IF_ALREADY_LOADED_PREFIX + fieldName, BasicType.BOOLEAN));
		il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFNE, _return));
		il.insert(_return, InstructionFactory.createThis());
		il.insert(_return, InstructionConst.DUP);
		il.insert(_return, InstructionConst.DUP);
		il.insert(_return, InstructionConst.ICONST_1);
		il.insert(_return, factory.createPutField(className, ClassInstrumentation.IF_ALREADY_LOADED_PREFIX + fieldName, BasicType.BOOLEAN));
		il.insert(_return, factory.createConstant(className));
		il.insert(_return, factory.createConstant(fieldName));
		il.insert(_return, factory.createConstant(field.getType().getName()));
		il.insert(_return, factory.createInvoke(className,
			fieldIsFinal ? ClassInstrumentation.DESERIALIZE_LAST_UPDATE_FOR_FINAL : ClassInstrumentation.DESERIALIZE_LAST_UPDATE_FOR,
			ObjectType.OBJECT, THREE_STRINGS_ARGS, Const.INVOKEVIRTUAL));
		il.insert(_return, factory.createCast(ObjectType.OBJECT, type));
		il.insert(_return, InstructionConst.DUP2);
		il.insert(_return, factory.createPutField(className, fieldName, type));
		il.insert(_return, factory.createPutField(className, ClassInstrumentation.OLD_PREFIX + fieldName, type));

		MethodGen ensureLoaded = new MethodGen(ClassInstrumentation.PRIVATE_SYNTHETIC, BasicType.VOID, Type.NO_ARGS, null,
			ClassInstrumentation.ENSURE_LOADED_PREFIX + fieldName, className, il, cpg);
		addMethod(ensureLoaded, true);
	}
}