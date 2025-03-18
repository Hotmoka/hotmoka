/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.internal.types;

import java.io.IOException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.gson.StorageTypeJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Shared code of the storage types.
 */
@Immutable
public abstract class AbstractStorageType extends AbstractMarshallable implements StorageType {

	@Override
	public final String getName() {
		return toString();
	}

	/**
	 * Creates a storage type corresponding to the given JSON description.
	 * 
	 * @param json the JSON description
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public static StorageType from(StorageTypeJson json) throws InconsistentJsonException {
		return named(json.getName(), InconsistentJsonException::new);
	}

	/**
	 * Yields the storage type unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the storage type
	 * @throws IOException if the type cannot be marshalled
	 */
	public static StorageType from(UnmarshallingContext context) throws IOException {
		byte selector = context.readByte();
	
		StorageType result = BasicTypeImpl.withSelector(selector);
		if (result != null)
			return result;
		else
			return Objects.requireNonNull(ClassTypeImpl.withSelector(selector, context), "Unknown storage type selector " + selector, IOException::new);
	}

	/**
	 * Yields the storage type with the given name.
	 * 
	 * @param <E> the type of the exception thrown if {@code name} is illegal for a storage type
	 * @param name the name of the type
	 * @param onIllegalName the supplier of the exception thrown if {@code name} is illegal for a storage type
	 * @return the storage type
	 * @throws E if {@code name} is illegal for a storage type
	 */
	public static <E extends Exception> StorageType named(String name, ExceptionSupplier<? extends E> onIllegalName) throws E {
		switch (Objects.requireNonNull(name, "name cannot be null", onIllegalName)) {
		case "boolean":
	        return BasicTypeImpl.BOOLEAN;
	    case "byte":
	        return BasicTypeImpl.BYTE;
	    case "char":
	        return BasicTypeImpl.CHAR;
	    case "short":
	        return BasicTypeImpl.SHORT;
	    case "int":
	        return BasicTypeImpl.INT;
	    case "long":
	        return BasicTypeImpl.LONG;
	    case "float":
	        return BasicTypeImpl.FLOAT;
	    case "double":
	        return BasicTypeImpl.DOUBLE;
	    default:
	    	return ClassTypeImpl.named(name, onIllegalName);
		}
	}

	/**
	 * Yields the storage type corresponding to the given class.
	 * 
	 * @param clazz the class
	 * @return the class type
	 */
	public static StorageType fromClass(Class<?> clazz) {
		if (Objects.requireNonNull(clazz, "clazz cannot be null", IllegalArgumentException::new) == boolean.class)
			return BasicTypeImpl.BOOLEAN;
		else if (clazz == byte.class)
			return BasicTypeImpl.BYTE;
		else if (clazz == char.class)
			return BasicTypeImpl.CHAR;
		else if (clazz == short.class)
			return BasicTypeImpl.SHORT;
		else if (clazz == int.class)
			return BasicTypeImpl.INT;
		else if (clazz == long.class)
			return BasicTypeImpl.LONG;
		else if (clazz == float.class)
			return BasicTypeImpl.FLOAT;
		else if (clazz == double.class)
			return BasicTypeImpl.DOUBLE;
		else if (clazz.isArray())
			throw new IllegalArgumentException("Arrays are not storage types");
		else
			return ClassTypeImpl.named(clazz.getName(), IllegalArgumentException::new);
	}
}