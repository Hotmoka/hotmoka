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
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.types.StorageType;

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
	 * Yields the storage type with the given name.
	 * 
	 * @param name the name of the type
	 * @return the storage type
	 */
	public static StorageType named(String name) {
		switch (name) {
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
	    	return ClassTypeImpl.named(name);
		}
	}

	/**
	 * Yields the storage type corresponding to the given class.
	 * 
	 * @param clazz the class
	 * @return the storage type
	 */
	public static StorageType fromClass(Class<?> clazz) {
		if (clazz == boolean.class)
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
		else
			return ClassTypeImpl.named(clazz.getName());
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

		result = ClassTypeImpl.withSelector(selector, context);
		if (result != null)
			return result;

		throw new IOException("Unexpected type selector: " + selector);
	}
}