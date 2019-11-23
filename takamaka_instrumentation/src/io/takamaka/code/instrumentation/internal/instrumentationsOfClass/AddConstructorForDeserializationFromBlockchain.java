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

import io.takamaka.code.instrumentation.internal.InstrumentedClass;
import io.takamaka.code.verification.Constants;

/**
 * An instrumentation that adds a constructor that deserializes an object of storage type. This
 * constructor receives the values of the eager fields, ordered by putting first
 * the fields of the superclasses, then those of the same class being
 * constructed, ordered by name and then by {@code toString()} of their type.
 */
public class AddConstructorForDeserializationFromBlockchain extends InstrumentedClass.Builder.ClassLevelInstrumentation {

	public AddConstructorForDeserializationFromBlockchain(InstrumentedClass.Builder builder) {
		builder.super();

		if (isStorage) {
			List<Type> args = new ArrayList<>();

			// the parameters of the constructor start with a storage reference to the object being deserialized
			args.add(new ObjectType(Constants.STORAGE_REFERENCE_NAME));

			// then there are the fields of the class and superclasses, with superclasses first
			if (!className.equals(Constants.STORAGE_NAME))
				eagerNonTransientInstanceFields.stream()
				.flatMap(SortedSet::stream)
				.map(Field::getType)
				.map(Type::getType)
				.forEachOrdered(args::add);

			InstructionList il = new InstructionList();
			int nextLocal = addCallToSuper(il);
			if (!className.equals(Constants.STORAGE_NAME))
				addInitializationOfEagerFields(il, nextLocal);

			il.append(InstructionConst.RETURN);

			MethodGen constructor = new MethodGen(InstrumentedClass.PUBLIC_SYNTHETIC, BasicType.VOID, args.toArray(Type.NO_ARGS), null,
					Const.CONSTRUCTOR_NAME, className, il, cpg);
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
		il.append(InstructionConst.ALOAD_1);
		argsForSuperclasses.add(new ObjectType(Constants.STORAGE_REFERENCE_NAME));

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
		};

		PushLoad pushLoad = new PushLoad();
		if (!className.equals(Constants.STORAGE_NAME))
			eagerNonTransientInstanceFields.stream().limit(eagerNonTransientInstanceFields.size() - 1)
				.flatMap(SortedSet::stream).map(Field::getType).map(Type::getType).forEachOrdered(pushLoad);

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
		Consumer<Field> putField = new Consumer<Field>() {
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
				il.append(factory.createPutField(className, InstrumentedClass.OLD_PREFIX + field.getName(), type));
				local += size;
			}
		};

		eagerNonTransientInstanceFields.getLast().forEach(putField);
	}
}