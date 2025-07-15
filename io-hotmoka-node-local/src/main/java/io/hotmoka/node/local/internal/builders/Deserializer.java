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

package io.hotmoka.node.local.internal.builders;

import java.lang.reflect.Constructor;
import java.lang.reflect.InaccessibleObjectException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.ByteValue;
import io.hotmoka.node.api.values.CharValue;
import io.hotmoka.node.api.values.DoubleValue;
import io.hotmoka.node.api.values.FloatValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.LongValue;
import io.hotmoka.node.api.values.NullValue;
import io.hotmoka.node.api.values.ShortValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.local.DeserializationException;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.whitelisting.Dummy;

/**
 * An implementation of an object that translates storage values into RAM values.
 */
public class Deserializer {

	/**
	 * The environment where deserialization is performed.
	 */
	private final ExecutionEnvironment environment;

	/**
	 * The class loader that can be used to load classes.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * A map from each storage reference to its deserialized object. This is needed in order to guarantee that
	 * repeated deserialization of the same storage reference yields the same object, but it is an efficiency measure as well.
	 */
	private final Map<StorageReference, Object> cache = new HashMap<>();

	/**
	 * Builds an object that translates storage values into RAM values.
	 * 
	 * @param environment the execution environment for which deserialization is performed
	 * @param classLoader the class loader that can be used to load classes during deserialization
	 */
	protected Deserializer(ExecutionEnvironment environment, EngineClassLoader classLoader) {
		this.environment = environment;
		this.classLoader = classLoader;
	}

	/**
	 * Deserializes the given storage value into its RAM image.
	 * 
	 * @param value the storage value
	 * @return the RAM image of {@code value}
	 */
	protected Object deserialize(StorageValue value) {
		if (value instanceof StorageReference sr) {
			// we use a cache to provide the same value if the same reference gets deserialized twice; putIfAbsent is clumsy because of the exceptions,
			// so we just get and put; in any case, this object is not meant to be thread-safe
			Object result = cache.get(sr);
			if (result == null)
				cache.put(sr, result = createStorageObject(sr));

			return result;
		}
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
			// two parameters passed from outside a node are never alias
			return new String(sv.getValue());
		else if (value instanceof BigIntegerValue biv)
			// we clone the value, so that the alias behavior of values coming from outside the node is fixed
			return new BigInteger(biv.getValue().toByteArray());
		else if (value == null)
			throw new LocalNodeException("Unexpected null storage value");
		else
			throw new LocalNodeException("Unexpected storage value of class " + value.getClass().getName());
	}

	/**
	 * A comparator that puts updates in the order required for the parameter of the
	 * deserialization constructor of storage objects: fields of superclasses first; then the
	 * fields for the same class, ordered by name and then by the {@code toString()} of their type.
	 * This ordering is the same used during instrumentation, when the deserialization constructor
	 * has been created: see {@link io.hotmoka.instrumentation.internal.InstrumentedClassImpl}.
	 */
	private int compare(UpdateOfField update1, UpdateOfField update2) {
		FieldSignature field1 = update1.getField();
		FieldSignature field2 = update2.getField();
		String className1 = field1.getDefiningClass().getName();
		String className2 = field2.getDefiningClass().getName();

		if (className1.equals(className2)) {
			int diff = field1.getName().compareTo(field2.getName());
			if (diff != 0)
				return diff;
			else
				return field1.getType().toString().compareTo(field2.getType().toString());
		}

		Class<?> clazz1, clazz2;

		try {
			clazz1 = classLoader.loadClass(className1);
		}
		catch (ClassNotFoundException e) {
			throw new DeserializationException("The object under deserialization contains a field " + field1 + " of an unknown class");
		}

		try {
			clazz2 = classLoader.loadClass(className2);
		}
		catch (ClassNotFoundException e) {
			throw new DeserializationException("The object under deserialization contains a field " + field2 + " of an unknown class");
		}

		if (clazz1.isAssignableFrom(clazz2)) // clazz1 superclass of clazz2
			return -1;
		else if (clazz2.isAssignableFrom(clazz1)) // clazz2 superclass of clazz1
			return 1;
		else
			throw new DeserializationException("Updates are not on the same supeclass chain");
	}

	/**
	 * Creates a storage object in RAM.
	 * 
	 * @param reference the reference of the object inside the node's store
	 * @return the object
	 */
	private Object createStorageObject(StorageReference reference) {
		List<Class<?>> formals = new ArrayList<>();
		List<Object> actuals = new ArrayList<>();
		// the constructor for deserialization has a first parameter
		// that receives the storage reference of the object
		formals.add(Object.class);
		actuals.add(reference);

		// we set the value for eager fields only; other fields will be loaded lazily;
		// we process the updates in the same order they have in the deserialization constructor
		ClassTag classTag;

		try {
			classTag = environment.getClassTag(reference);
		}
		catch (UnknownReferenceException e) {
			throw new DeserializationException("Cannot deserialize " + reference + ": it is not in store");
		}

		UpdateOfField[] eagerUpdates;

		try {
			eagerUpdates = environment.getEagerFields(reference)
				.sorted(this::compare)
				.toArray(UpdateOfField[]::new);
		}
		catch (UnknownReferenceException e) {
			// we managed to compute its class tag above, so this is a problem of the store
			throw new LocalNodeException(e);
		}

		for (var update: eagerUpdates) {
			try {
				formals.add(classLoader.loadClass(update.getField().getType()));
				actuals.add(deserialize(update.getValue()));
			}
			catch (ClassNotFoundException e) {
				throw new DeserializationException("Cannot deserialize " + reference + ": it has a field " + update.getField() + " of an unknown class");
			}
		}

		Class<?> clazz;

		try {
			clazz = classLoader.loadClass(classTag.getClazz().getName());
		}
		catch (ClassNotFoundException e) {
			throw new DeserializationException("Cannot resolve class " + e.getMessage());
		}

		// the classloader of a deserializer is built for the classpath of a transaction request (without any
		// explicit jar): all classes must have been installed in one of the transaction references of that classpath
		TransactionReference actual = classLoader.transactionThatInstalledJarFor(clazz)
			.orElseThrow(() -> new LocalNodeException("Class " + clazz.getName() + " was a storage class, therefore it should have been installed in the store with some jar"));
		TransactionReference expected = classTag.getJar();
		if (!actual.equals(expected))
			// this means that the deserializer has been built for a classpath inconsistent with that used for creating the object:
			// it is not a bug in the code of Hotmoka, it is an incorrect specification of the classpath, itself
			// contained in a user request; therefore, the exception is checked
			throw new DeserializationException("Class " + classTag.getClazz() + " was instantiated from jar at " + expected + " not from jar at " + actual);

		// we add the fictitious argument that avoids name clashes
		formals.add(Dummy.class);
		actuals.add(null);

		Constructor<?> constructor;

		try {
			constructor = clazz.getConstructor(formals.toArray(Class[]::new));
			// the instrumented constructor is public, but the class might well be non-public; hence we must force accessibility
			constructor.setAccessible(true);
		}
		catch (NoSuchMethodException | SecurityException | InaccessibleObjectException e) {
			// the instrumented constructor is missing or not accessible: the store is corrupted
			throw new LocalNodeException(e);
		}

		try {
			return constructor.newInstance(actuals.toArray(Object[]::new));
		}
		catch (ReflectiveOperationException | RuntimeException e) {
			// the instrumented constructor should work without exceptions, otherwise the store is corrupted
			throw new LocalNodeException(e);
		}
	}
}