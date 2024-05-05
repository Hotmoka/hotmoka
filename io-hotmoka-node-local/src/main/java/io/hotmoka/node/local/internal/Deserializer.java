/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.local.internal;

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

import io.hotmoka.node.DeserializationError;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.ByteValue;
import io.hotmoka.node.api.values.CharValue;
import io.hotmoka.node.api.values.DoubleValue;
import io.hotmoka.node.api.values.EnumValue;
import io.hotmoka.node.api.values.FloatValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.LongValue;
import io.hotmoka.node.api.values.NullValue;
import io.hotmoka.node.api.values.ShortValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreTransaction;
import io.hotmoka.node.local.internal.transactions.AbstractResponseBuilder;
import io.hotmoka.whitelisting.Dummy;

/**
 * An implementation of an object that translates storage values into RAM values.
 */
public class Deserializer {

	/**
	 * The transaction for which deserialization is performed.
	 */
	private final StoreTransaction<?> storeTransaction;

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
			if (update1 instanceof UpdateOfField uof1 && update2 instanceof UpdateOfField uof2) {
				FieldSignature field1 = uof1.getField();
				FieldSignature field2 = uof2.getField();

				try {
					String className1 = field1.getDefiningClass().getName();
					String className2 = field2.getDefiningClass().getName();

					if (className1.equals(className2)) {
						int diff = field1.getName().compareTo(field2.getName());
						if (diff != 0)
							return diff;
						else
							return field1.getType().toString().compareTo(field2.getType().toString()); // TODO: types are comparable!
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
	 */
	public Deserializer(AbstractResponseBuilder<?,?> builder) {
		this.storeTransaction = builder.storeTransaction;
		this.classLoader = builder.classLoader;
	}

	/**
	 * Deserializes the given storage value into its RAM image.
	 * 
	 * @param value the storage value
	 * @return the RAM image of {@code value}
	 */
	public Object deserialize(StorageValue value) {
		if (value instanceof StorageReference sr)
			// we use a cache to provide the same value if the same reference gets deserialized twice
			return cache.computeIfAbsent(sr, this::createStorageObject);
		else if (value instanceof IntValue iv)
			return iv.getValue();
		else if (value instanceof BooleanValue bv)
			return bv.getValue();
		else if (value instanceof LongValue lv)
			return lv.getValue();
		else if (value instanceof NullValue)
			return null;
		else if (value instanceof ByteValue bv)
			return bv.getValue();
		else if (value instanceof ShortValue sv)
			return sv.getValue();
		else if (value instanceof CharValue cv)
			return cv.getValue();
		else if (value instanceof FloatValue fv)
			return fv.getValue();
		else if (value instanceof DoubleValue dv)
			return dv.getValue();
		else if (value instanceof StringValue sv)
			// we clone the value, so that the alias behavior of values coming from outside the node is fixed:
			// two parameters of an entry are never alias when they come from outside the node
			return new String(sv.getValue());
		else if (value instanceof BigIntegerValue biv)
			// we clone the value, so that the alias behavior of values coming from outside the node is fixed
			return new BigInteger(biv.getValue().toByteArray());
		else if (value instanceof EnumValue ev) {
			try {
				// below, we cannot use:
				// return Enum.valueOf((Class<? extends Enum>) classLoader.loadClass(ev.enumClassName), ev.name);
				// since that method internally calls by reflection the valueOf() method of the enum,
				// which is instrumented and hence will crash; we need a long alternative instead:
				Class<?> enumClass = classLoader.loadClass(ev.getEnumClassName());
				Optional<Field> fieldOfElement = Stream.of(enumClass.getDeclaredFields())
					.filter(field -> Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
					.filter(field -> field.getName().equals(ev.getName()))
					.filter(field -> field.getType() == enumClass)
					.findFirst();

				Field field = fieldOfElement.orElseThrow(() -> new DeserializationError("Cannot find enum constant " + ev.getName()));
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
			ClassTag classTag = storeTransaction.getClassTagUncommitted(reference);
			storeTransaction.getEagerFieldsUncommitted(reference)
				.sorted(updateComparator)
				.forEachOrdered(update -> {
					try {
						formals.add(classLoader.loadClass(update.getField().getType()));
						actuals.add(deserialize(update.getValue()));
					}
					catch (ClassNotFoundException e) {
						throw new DeserializationError(e);
					}
				});
	
			Class<?> clazz = classLoader.loadClass(classTag.getClazz().getName());
			TransactionReference actual = classLoader.transactionThatInstalledJarFor(clazz);
			TransactionReference expected = classTag.getJar();
			if (!actual.equals(expected))
				throw new DeserializationError("Class " + classTag.getClazz() + " was instantiated from jar at " + expected + " not from jar at " + actual);
	
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