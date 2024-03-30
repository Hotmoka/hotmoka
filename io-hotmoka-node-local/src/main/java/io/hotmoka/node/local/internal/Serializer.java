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

import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.node.local.internal.transactions.AbstractResponseBuilder;
import io.takamaka.code.constants.Constants;

/**
 * An object that translates RAM values into storage values.
 */
public class Serializer {

	/**
	 * The builder of the transaction for which serialization is performed.
	 */
	private final AbstractResponseBuilder<?,?> builder;

	/**
	 * Builds an object that translates RAM values into storage values.
	 * 
	 * @param builder the builder of the transaction for which serialization is performed
	 */
	public Serializer(AbstractResponseBuilder<?,?> builder) {
		this.builder = builder;
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
			return builder.classLoader.getStorageReferenceOf(object);
		else if (object instanceof BigInteger)
			return StorageValues.bigIntegerOf((BigInteger) object);
		else if (object instanceof Boolean)
			return StorageValues.booleanOf((Boolean) object);
		else if (object instanceof Byte)
			return StorageValues.byteOf((Byte) object);
		else if (object instanceof Character)
			return StorageValues.charOf((Character) object);
		else if (object instanceof Double)
			return StorageValues.doubleOf((Double) object);
		else if (object instanceof Float)
			return StorageValues.floatOf((Float) object);
		else if (object instanceof Integer)
			return StorageValues.intOf((Integer) object);
		else if (object instanceof Long)
			return StorageValues.longOf((Long) object);
		else if (object instanceof Short)
			return StorageValues.shortOf((Short) object);
		else if (object instanceof String)
			return StorageValues.stringOf((String) object);
		else if (object instanceof Enum<?>)
			return StorageValues.enumElementOf(object.getClass().getName(), ((Enum<?>) object).name());
		else if (object == null)
			return StorageValues.NULL;
		else
			throw new IllegalArgumentException("An object of class " + object.getClass().getName()
				+ " cannot be kept in store since it does not implement " + Constants.STORAGE_NAME);
	}

	private boolean isStorage(Object object) {
		return object != null && builder.classLoader.getStorage().isAssignableFrom(object.getClass());
	}
}