package takamaka.translator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import takamaka.blockchain.StorageReference;
import takamaka.lang.Storage;

class ClassInstrumentation {
	private final static Logger LOGGER = Logger.getLogger(ClassInstrumentation.class.getName());
	private final static String OLD_PREFIX = "§old_";
	private final static String IF_ALREADY_LOADED_PREFIX = "§ifAlreadyLoaded_";

	/**
	 * The order used for generating the parameters of the instrumented constructors.
	 */
	private final static Comparator<Field> fieldOrder = Comparator.comparing(Field::getName).thenComparing(field -> field.getType().toString());

	public ClassInstrumentation(InputStream input, String className, JarOutputStream instrumentedJar, Program program) throws ClassFormatException, IOException {
		LOGGER.fine(() -> "Instrumenting " + className);
		ClassGen classGen = new ClassGen(new ClassParser(input, className).parse());
		new Initializer(classGen, program);
		classGen.getJavaClass().dump(instrumentedJar);
	}

	private class Initializer {
		/**
		 * The class that is being instrumented.
		 */
		private final ClassGen classGen;

		/**
		 * The name of <code>classGen</code>.
		 */
		private final String className;

		/**
		 * The constant pool of the class being instrumented.
		 */
		private final ConstantPoolGen cpg;

		/**
		 * True if and only if <code>classGen</code> is a storage class.
		 */
		private final boolean isStorage;

		/**
		 * The non-transient instance fields of primitive type defined in <code>classGen</code>
		 * and in its superclasses up to Storage (excluded). This is non-empty for storage classes only.
		 */
		private final LinkedList<SortedSet<Field>> primitiveNonTransientInstanceFields = new LinkedList<>();

		/**
		 * The non-transient instance fields of reference type defined in <code>classGen</code>
		 * (superclasses are not considered). This is non-empty for storage classes only.
		 */
		private final SortedSet<Field> referenceNonTransientInstanceFields = new TreeSet<>(fieldOrder);

		/**
		 * The program that collects the classes under instrumentation and those of the
		 * supporting libraries.
		 */
		private final Program program;

		private Initializer(ClassGen classGen, Program program) {
			this.classGen = classGen;
			this.className = classGen.getClassName();
			this.cpg = classGen.getConstantPool();
			this.program = program;
			this.isStorage = isStorage(className);
			if (isStorage)
				collectPrimitiveNonTransientInstanceFieldsOf(className);
			
			instrument();
		}

		private void instrument() {
			if (isStorage) {
				addOldAndIfAlreadyLoadedFields();
				addConstructorForDeserializationFromBlockchain();
			}
			//TODO
		}

		private void addOldAndIfAlreadyLoadedFields() {
			for (Field field: primitiveNonTransientInstanceFields.getLast())
				addOldFieldFor(field);

			for (Field field: referenceNonTransientInstanceFields) {
				addOldFieldFor(field);
				addIfAlreadyLoadedFieldFor(field);
			}
		}

		private void addIfAlreadyLoadedFieldFor(Field field) {
			FieldGen ifAlreadyLoaded = new FieldGen(Const.ACC_PRIVATE | Const.ACC_SYNTHETIC, BasicType.BOOLEAN, IF_ALREADY_LOADED_PREFIX + field.getName(), cpg);
			classGen.addField(ifAlreadyLoaded.getField());
		}

		private void addOldFieldFor(Field field) {
			FieldGen copy = new FieldGen(field, cpg);
			copy.setName(OLD_PREFIX + field.getName());
			copy.setAccessFlags(Const.ACC_PRIVATE | Const.ACC_SYNTHETIC);
			classGen.addField(copy.getField());
		}

		private void addConstructorForDeserializationFromBlockchain() {
			InstructionList il = new InstructionList();
			il.append(InstructionConst.RETURN);
			
			List<Type> args = new ArrayList<>();
			List<String> names = new ArrayList<>();
			args.add(new ObjectType(StorageReference.class.getName()));
			names.add("storageReference");
			//TODO

			MethodGen constructor = new MethodGen(Const.ACC_PUBLIC | Const.ACC_SYNTHETIC, BasicType.VOID, args.toArray(new Type[0]), names.toArray(new String[0]), Const.CONSTRUCTOR_NAME, className, il, cpg);
			classGen.addMethod(constructor.getMethod());
		}

		private void collectPrimitiveNonTransientInstanceFieldsOf(String className) {
			if (!className.equals(Storage.class.getName())) {
				JavaClass clazz = program.get(className);
				if (clazz != null) {
					// we put at the beginning the fields of the superclasses
					collectPrimitiveNonTransientInstanceFieldsOf(clazz.getSuperclassName());

					// then the fields of className, in order
					primitiveNonTransientInstanceFields.add(Stream.of(clazz.getFields())
						.filter(field -> !field.isStatic() && !field.isTransient() && field.getType() instanceof BasicType)
						.collect(Collectors.toCollection(() -> new TreeSet<>(fieldOrder))));

					// we collect reference fields as well, but only for the class being instrumented
					if (className.equals(this.className))
						Stream.of(clazz.getFields())
							.filter(field -> !field.isStatic() && !field.isTransient() && field.getType() instanceof ReferenceType)
							.forEach(referenceNonTransientInstanceFields::add);
				}
			}
		}

		private boolean isStorage(String className) {
			if (className.equals(Storage.class.getName()))
				return true;
			else {
				JavaClass clazz = program.get(className);
				if (clazz == null)
					return false;
				else {
					String superclassName = clazz.getSuperclassName();
					return superclassName != null && isStorage(superclassName);
				}
			}
		}
	}
}