package io.takamaka.code.blockchain.internal;

import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.DeserializationError;
import io.takamaka.code.blockchain.TransactionReference;
import io.takamaka.code.blockchain.signatures.FieldSignature;
import io.takamaka.code.blockchain.updates.ClassTag;
import io.takamaka.code.blockchain.updates.Update;
import io.takamaka.code.blockchain.updates.UpdateOfField;
import io.takamaka.code.blockchain.values.BigIntegerValue;
import io.takamaka.code.blockchain.values.BooleanValue;
import io.takamaka.code.blockchain.values.ByteValue;
import io.takamaka.code.blockchain.values.CharValue;
import io.takamaka.code.blockchain.values.DoubleValue;
import io.takamaka.code.blockchain.values.EnumValue;
import io.takamaka.code.blockchain.values.FloatValue;
import io.takamaka.code.blockchain.values.IntValue;
import io.takamaka.code.blockchain.values.LongValue;
import io.takamaka.code.blockchain.values.NullValue;
import io.takamaka.code.blockchain.values.ShortValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StorageValue;
import io.takamaka.code.blockchain.values.StringValue;
import io.takamaka.code.verification.Dummy;

public class Deserializer {

	/**
	 * The blockchain for which deserialization is performed.
	 */
	private AbstractBlockchain blockchain;

	/**
	 * A map from each storage reference to its deserialized object. This is needed in order to guarantee that
	 * repeated deserialization of the same storage reference yields the same object and can also
	 * work as an efficiency measure. This is reset at each transaction since each transaction uses
	 * a distinct class loader and each storage object keeps a reference to its class loader, as
	 * always in Java.
	 */
	private final Map<StorageReference, Object> cache = new HashMap<>();

	/**
	 * A comparator that puts updates in the order required for the parameter
	 * of the deserialization constructor of storage objects: fields of superclasses first;
	 * then the fields for the same class, ordered by name and then by the
	 * {@code toString()} of their type.
	 */
	private final Comparator<Update> updateComparator = new Comparator<Update>() {

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

					Class<?> clazz1 = blockchain.loadClass(className1);
					Class<?> clazz2 = blockchain.loadClass(className2);
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

	public static interface GetLastEagerUpdatesFor {
		Stream<Update> apply(StorageReference storageReference) throws Exception;
	}

	/**
	 * A function that yields the last updates for the eager fields of a storage reference.
	 */
	private final GetLastEagerUpdatesFor getLastEagerUpdatesFor;

	/**
	 * Builds an object that translates storage values into RAM values.
	 * 
	 * @param blockchain the blockchain for which deserialization is performed
	 * @param a function that yields the last updates for the eager fields of a storage reference
	 */
	public Deserializer(AbstractBlockchain blockchain, GetLastEagerUpdatesFor getLastEagerUpdatesFor) {
		this.blockchain = blockchain;
		this.getLastEagerUpdatesFor = getLastEagerUpdatesFor;
	}

	public void init() {
		cache.clear();
	}

	/**
	 * Yields the deserialization of the given value. That is, it yields an actual object in RAM
	 * that reflects its representation in blockchain.
	 * 
	 * @param value the value to deserialize
	 * @return the deserialization of {@code value}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object deserialize(StorageValue value) {
		if (value instanceof StorageReference)
			// we use a cache to provide the same value if the same reference gets deserialized twice
			return cache.computeIfAbsent((StorageReference) value, this::deserializeAnew);
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
			// we clone the value, so that the alias behavior of values coming from outside the blockchain is fixed:
			// two parameters of an entry are never alias when they come from outside the blockchain
			return new String(((StringValue) value).value);
		else if (value instanceof BigIntegerValue)
			// we clone the value, so that the alias behavior of values coming from outside the blockchain is fixed
			return new BigInteger(value.toString());
		else if (value instanceof EnumValue) {
			EnumValue ev = (EnumValue) value;

			try {
				return Enum.valueOf((Class<? extends Enum>) blockchain.loadClass(ev.enumClassName), ev.name);
			}
			catch (ClassNotFoundException e) {
				throw new DeserializationError(e);
			}
		}
		else
			throw new DeserializationError("unexpected storage value");
	}

	/**
	 * Deserializes the given storage reference from the blockchain.
	 * 
	 * @param reference the storage reference to deserialize
	 * @return the resulting storage object
	 */
	private Object deserializeAnew(StorageReference reference) {
		try {
			return createStorageObject(reference, getLastEagerUpdatesFor.apply(reference));
		}
		catch (DeserializationError e) {
			throw e;
		}
		catch (Exception e) {
			throw new DeserializationError(e);
		}
	}

	/**
	 * Creates a storage object in RAM.
	 * 
	 * @param reference the blockchain reference of the object
	 * @param updates the eager updates of the object, including its class tag
	 * @return the object
	 * @throws DeserializationError if the object could not be created
	 */
	private Object createStorageObject(StorageReference reference, Stream<Update> updates) {
		try {
			ClassTag classTag = null;
			List<Class<?>> formals = new ArrayList<>();
			List<Object> actuals = new ArrayList<>();
			// the constructor for deserialization has a first parameter
			// that receives the storage reference of the object
			formals.add(Object.class);
			actuals.add(reference);

			// we process the updates in the same order they have in the deserialization constructor
			for (Update update: updates.collect(Collectors.toCollection(() -> new TreeSet<>(updateComparator))))
				if (update instanceof ClassTag)
					classTag = (ClassTag) update;
				else {
					UpdateOfField updateOfField = (UpdateOfField) update;
					formals.add(updateOfField.getField().type.toClass(blockchain));
					actuals.add(deserialize(updateOfField.getValue()));
				}
	
			if (classTag == null)
				throw new DeserializationError("No class tag found for " + reference);

			Class<?> clazz = blockchain.loadClass(classTag.className);
			TransactionReference actual = blockchain.transactionThatInstalledJarFor(clazz);
			TransactionReference expected = classTag.jar;
			if (!actual.equals(expected))
				throw new DeserializationError("Class " + classTag.className + " was instantiated from jar at " + expected + " not from jar at " + actual);

			// we add the fictitious argument that avoids name clashes
			formals.add(Dummy.class);
			actuals.add(null);

			Constructor<?> constructor = clazz.getConstructor(formals.toArray(new Class<?>[formals.size()]));

			// the instrumented constructor is public, but the class might well be non-public;
			// hence we must force accessibility
			constructor.setAccessible(true);

			return constructor.newInstance(actuals.toArray(new Object[actuals.size()]));
		}
		catch (DeserializationError e) {
			throw e;
		}
		catch (Exception e) {
			throw new DeserializationError(e);
		}
	}
}