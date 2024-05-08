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

import java.math.BigInteger;

import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.takamaka.code.constants.Constants;

/**
 * An object that translates RAM values into storage values.
 */
public class Serializer {

	private final EngineClassLoader classLoader;

	/**
	 * Builds an object that translates RAM values into storage values.
	 * 
	 * @param builder the builder of the transaction for which serialization is performed
	 */
	public Serializer(EngineClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Yields the serialization of the given RAM object, that is, yields its
	 * representation in the node's store.
	 * 
	 * @param object the object to serialize. This must be a storage object, a Java wrapper
	 *               object for numerical types, an enumeration
	 *               or a special Java object that is allowed
	 *               in store, such as a {@link java.lang.String} or {@link java.math.BigInteger}
	 * @return the serialization of {@code object}, if any
	 * @throws IllegalArgumentException if the type of {@code object} is not allowed in store
	 */
	public StorageValue serialize(Object object) throws IllegalArgumentException {
		if (isStorage(object))
			return classLoader.getStorageReferenceOf(object);
		else if (object instanceof BigInteger bi)
			return StorageValues.bigIntegerOf(bi);
		else if (object instanceof Boolean b)
			return StorageValues.booleanOf(b);
		else if (object instanceof Byte b)
			return StorageValues.byteOf(b);
		else if (object instanceof Character c)
			return StorageValues.charOf(c);
		else if (object instanceof Double d)
			return StorageValues.doubleOf(d);
		else if (object instanceof Float f)
			return StorageValues.floatOf(f);
		else if (object instanceof Integer i)
			return StorageValues.intOf(i);
		else if (object instanceof Long l)
			return StorageValues.longOf(l);
		else if (object instanceof Short s)
			return StorageValues.shortOf(s);
		else if (object instanceof String s)
			return StorageValues.stringOf(s);
		else if (object instanceof Enum<?> e)
			return StorageValues.enumElementOf(e.getClass().getName(), e.name());
		else if (object == null)
			return StorageValues.NULL;
		else
			throw new IllegalArgumentException("An object of class " + object.getClass().getName()
				+ " cannot be kept in store since it does not implement " + Constants.STORAGE_NAME);
	}

	private boolean isStorage(Object object) {
		return object != null && classLoader.getStorage().isAssignableFrom(object.getClass());
	}
}