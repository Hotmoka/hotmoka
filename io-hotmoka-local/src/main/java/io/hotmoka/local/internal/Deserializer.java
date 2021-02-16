package io.hotmoka.local.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.ByteValue;
import io.hotmoka.beans.values.CharValue;
import io.hotmoka.beans.values.DoubleValue;
import io.hotmoka.beans.values.EnumValue;
import io.hotmoka.beans.values.FloatValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.NullValue;
import io.hotmoka.beans.values.ShortValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.local.EngineClassLoader;
import io.hotmoka.local.StoreUtilities;
import io.hotmoka.local.internal.transactions.AbstractResponseBuilder;
import io.hotmoka.nodes.DeserializationError;
import io.takamaka.code.verification.Dummy;

/**
 * An implementation of an object that translates storage values into RAM values.
 */
public class Deserializer {

	/**
	 * The store utilities of the node.
	 */
	private final StoreUtilities storeUtilities;

	/**
	 * The object that translates storage types into their run-time class tag.
	 */
	private final StorageTypeToClass storageTypeToClass;

	/**
	 * The class loader that can be used to load classes.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * A map from each storage reference to its deserialized object. This is needed in order to guarantee that
	 * repeated deserialization of the same storage reference yields the same object and can also
	 * work as an efficiency measure.
	 */
	private final Map<StorageReference, Object> cache = new HashMap<>();

	/**
	 * A comparator that puts updates in the order required for the parameter
	 * of the deserialization constructor of storage objects: fields of superclasses first;
	 * then the fields for the same class, ordered by name and then by the
	 * {@code toString()} of their type.
	 */
	private final Comparator<Update> updateComparator = new Comparator<>() {

		@Override
		public int compare(Update update1, Update update2) {
			if (update1 instanceof UpdateOfField && update2 instanceof UpdateOfField) {
				FieldSignature field1 = ((UpdateOfField) update1).getField();
				FieldSignature field2 = ((UpdateOfField) update2).getField();

				try {
					String className1 = field1.definingClass.name;
					String className2 = field2.definingClass.name;

					if (className1.equals(className2)) {
						int diff = field1.name.compareTo(field2.name);
						if (diff != 0)
							return diff;
						else
							return field1.type.toString().compareTo(field2.type.toString());
					}

					Class<?> clazz1 = classLoader.loadClass(className1);
					Class<?> clazz2 = classLoader.loadClass(className2);
					if (clazz1.isAssignableFrom(clazz2)) // clazz1 superclass of clazz2
						return -1;
					else if (clazz2.isAssignableFrom(clazz1)) // clazz2 superclass of clazz1
						return 1;
					else
						throw new IllegalStateException("Updates are not on the same supeclass chain");
				}
				catch (ClassNotFoundException e) {
					throw new DeserializationError(e);
				}
			}
			else
				return update1.compareTo(update2);
		}
	};

	/**
	 * Builds an object that translates storage values into RAM values.
	 * 
	 * @param builder the response builder for which deserialization is performed
	 * @param storeUtilities the store utilities of the node
	 */
	public Deserializer(AbstractResponseBuilder<?,?> builder, StoreUtilities storeUtilities) {
		this.storeUtilities = storeUtilities;
		this.storageTypeToClass = builder.storageTypeToClass;
		this.classLoader = builder.classLoader;
	}

	/**
	 * Deserializes the given storage value into its RAM image.
	 * 
	 * @param value the storage value
	 * @return the RAM image of {@code value}
	 */
	public Object deserialize(StorageValue value) {
		if (value instanceof StorageReference)
			// we use a cache to provide the same value if the same reference gets deserialized twice
			return cache.computeIfAbsent((StorageReference) value, this::createStorageObject);
		else if (value instanceof IntValue)
			return ((IntValue) value).value;
		else if (value instanceof BooleanValue)
			return ((BooleanValue) value).value;
		else if (value instanceof LongValue)
			return ((LongValue) value).value;
		else if (value instanceof NullValue)
			return null;
		else if (value instanceof ByteValue)
			return ((ByteValue) value).value;
		else if (value instanceof ShortValue)
			return ((ShortValue) value).value;
		else if (value instanceof CharValue)
			return ((CharValue) value).value;
		else if (value instanceof FloatValue)
			return ((FloatValue) value).value;
		else if (value instanceof DoubleValue)
			return ((DoubleValue) value).value;
		else if (value instanceof StringValue)
			// we clone the value, so that the alias behavior of values coming from outside the node is fixed:
			// two parameters of an entry are never alias when they come from outside the node
			return new String(((StringValue) value).value);
		else if (value instanceof BigIntegerValue)
			// we clone the value, so that the alias behavior of values coming from outside the node is fixed
			return new BigInteger(value.toString());
		else if (value instanceof EnumValue) {
			EnumValue ev = (EnumValue) value;

			try {
				// below, we cannot use:
				// return Enum.valueOf((Class<? extends Enum>) classLoader.loadClass(ev.enumClassName), ev.name);
				// since that method internally calls by reflection the valueOf() method of the enum,
				// which is instrumented and hence will crash; we need a long alternative instead:
				Class<?> enumClass = classLoader.loadClass(ev.enumClassName);
				Optional<Field> fieldOfElement = Stream.of(enumClass.getDeclaredFields())
					.filter(field -> Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
					.filter(field -> field.getName().equals(ev.name))
					.filter(field -> field.getType() == enumClass)
					.findFirst();

				Field field = fieldOfElement.orElseThrow(() -> new DeserializationError("cannot find enum constant " + ev.name));
				// the field is public, but the class might not be public
				field.setAccessible(true);

				return field.get(null);
			}
			catch (ClassNotFoundException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new DeserializationError(e);
			}
		}
		else
			throw new DeserializationError("unexpected storage value");
	}

	/**
	 * Creates a storage object in RAM.
	 * 
	 * @param reference the reference of the object inside the node's store
	 * @return the object
	 * @throws DeserializationError if the object could not be created
	 */
	private Object createStorageObject(StorageReference reference) {
		try {
			List<Class<?>> formals = new ArrayList<>();
			List<Object> actuals = new ArrayList<>();
			// the constructor for deserialization has a first parameter
			// that receives the storage reference of the object
			formals.add(Object.class);
			actuals.add(reference);
	
			// we set the value for eager fields only; other fields will be loaded lazily
			// we process the updates in the same order they have in the deserialization constructor
			ClassTag classTag = storeUtilities.getClassTagUncommitted(reference);
			storeUtilities.getEagerFieldsUncommitted(reference)
				.sorted(updateComparator)
				.forEachOrdered(update -> {
					try {
						formals.add(storageTypeToClass.toClass(update.getField().type));
						actuals.add(deserialize(update.getValue()));
					}
					catch (ClassNotFoundException e) {
						throw new DeserializationError(e);
					}
				});
	
			Class<?> clazz = classLoader.loadClass(classTag.clazz.name);
			TransactionReference actual = classLoader.transactionThatInstalledJarFor(clazz);
			TransactionReference expected = classTag.jar;
			if (!actual.equals(expected))
				throw new DeserializationError("Class " + classTag.clazz + " was instantiated from jar at " + expected + " not from jar at " + actual);
	
			// we add the fictitious argument that avoids name clashes
			formals.add(Dummy.class);
			actuals.add(null);
	
			Constructor<?> constructor = clazz.getConstructor(formals.toArray(Class[]::new));
	
			// the instrumented constructor is public, but the class might well be non-public; hence we must force accessibility
			constructor.setAccessible(true);
	
			return constructor.newInstance(actuals.toArray(Object[]::new));
		}
		catch (DeserializationError e) {
			throw e;
		}
		catch (Exception e) {
			throw new DeserializationError(e);
		}
	}
}