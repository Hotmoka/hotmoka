package io.takamaka.code.instrumentation.internal.instrumentationsOfClass;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Consumer;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.instrumentation.internal.InstrumentedClassImpl;
import io.takamaka.code.verification.Dummy;
import io.takamaka.code.instrumentation.InstrumentationConstants;

/**
 * An instrumentation that adds a constructor that deserializes an object of storage type. This
 * constructor receives the values of the eager fields, ordered by putting first
 * the fields of the superclasses, then those of the same class being
 * constructed, ordered by name and then by {@code toString()} of their type.
 */
public class AddConstructorForDeserializationFromStore extends InstrumentedClassImpl.Builder.ClassLevelInstrumentation {
	private final static short PUBLIC_SYNTHETIC = Const.ACC_PUBLIC | Const.ACC_SYNTHETIC;

	public AddConstructorForDeserializationFromStore(InstrumentedClassImpl.Builder builder) {
		builder.super();

		if (isStorage) {
			List<Type> args = new ArrayList<>();

			// the parameters of the constructor start with a storage reference to the object being deserialized
			args.add(Type.OBJECT);

			// then there are the fields of the class and superclasses, with superclasses first
			eagerNonTransientInstanceFields.stream()
				.flatMap(SortedSet::stream)
				.map(Field::getType)
				.map(Type::getType)
				.forEachOrdered(args::add);

			// at the end, there is a fictitious argument used to avoid clashes with
			// already existing, user-provided constructors
			args.add(new ObjectType(Dummy.class.getName()));

			InstructionList il = new InstructionList();
			int nextLocal = addCallToSuper(il);
			addInitializationOfEagerFields(il, nextLocal);

			if (className.equals(io.takamaka.code.constants.Constants.STORAGE_NAME)) {
				// the Storage class needs to initialize its two synthetic transient fields
				il.append(InstructionFactory.createThis());
				il.append(factory.createConstant(true));
				il.append(factory.createPutField(className, InstrumentationConstants.IN_STORAGE, Type.BOOLEAN));
				il.append(InstructionFactory.createThis());
				il.append(InstructionConst.ALOAD_1); // the first parameter: the storage reference
				il.append(factory.createPutField(className, InstrumentationConstants.STORAGE_REFERENCE_FIELD_NAME, Type.OBJECT));	
			}

			il.append(InstructionConst.RETURN);

			MethodGen constructor = new MethodGen(PUBLIC_SYNTHETIC, BasicType.VOID, args.toArray(Type.NO_ARGS), null, Const.CONSTRUCTOR_NAME, className, il, cpg);
			addMethod(constructor, false);
		}
	}

	/**
	 * Adds a call from the deserialization constructor of a storage class to the
	 * deserialization constructor of the superclass.
	 * 
	 * @param il the instructions where the call must be added
	 * @return the number of local variables used to accomodate the arguments passed to the constructor of the superclass
	 */
	private int addCallToSuper(InstructionList il) {
		List<Type> argsForSuperclasses = new ArrayList<>();
		il.append(InstructionFactory.createThis());

		// the Storage class does not pass the storage reference upwards
		if (!className.equals(io.takamaka.code.constants.Constants.STORAGE_NAME)) {
			il.append(InstructionConst.ALOAD_1);
			argsForSuperclasses.add(ObjectType.OBJECT);
		}

		// the fields of the superclasses are passed into a call to super(...)
		class PushLoad implements Consumer<Type> {
			// the first two slots are used for this and the storage reference
			private int local = 2;

			@Override
			public void accept(Type type) {
				argsForSuperclasses.add(type);
				il.append(InstructionFactory.createLoad(type, local));
				local += type.getSize();
			}
		}

		PushLoad pushLoad = new PushLoad();
		// we push the value of all eager fields but in superclasses only
		eagerNonTransientInstanceFields.stream().limit(eagerNonTransientInstanceFields.size() - 1)
			.flatMap(SortedSet::stream).map(Field::getType).map(Type::getType).forEachOrdered(pushLoad);

		if (!className.equals(io.takamaka.code.constants.Constants.STORAGE_NAME)) {
			// we pass null for the dummy argument
			il.append(InstructionConst.ACONST_NULL);
			argsForSuperclasses.add(new ObjectType(Dummy.class.getName()));
		}

		il.append(factory.createInvoke(getSuperclassName(), Const.CONSTRUCTOR_NAME, BasicType.VOID,
			argsForSuperclasses.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

		return pushLoad.local;
	}

	/**
	 * Adds code that initializes the eager fields of the storage class being instrumented.
	 * 
	 * @param il the instructions where the code must be added
	 * @param nextLocal the local variables where the parameters start, that must be stored in the fields
	 */
	private void addInitializationOfEagerFields(InstructionList il, int nextLocal) {
		Consumer<Field> putField = new Consumer<>() {
			private int local = nextLocal;

			@Override
			public void accept(Field field) {
				Type type = Type.getType(field.getType());
				int size = type.getSize();
				il.append(InstructionFactory.createThis());
				il.append(InstructionFactory.createLoad(type, local));

				// we reduce the size of the code for the frequent case of one slot values
				if (size == 1)
					il.append(InstructionConst.DUP2);
				il.append(factory.createPutField(className, field.getName(), type));
				if (size != 1) {
					il.append(InstructionFactory.createThis());
					il.append(InstructionFactory.createLoad(type, local));
				}
				il.append(factory.createPutField(className, InstrumentationConstants.OLD_PREFIX + field.getName(), type));
				local += size;
			}
		};

		eagerNonTransientInstanceFields.getLast().forEach(putField);
	}
}