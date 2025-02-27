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

package io.hotmoka.verification.internal;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.hotmoka.verification.api.BcelToClass;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.api.VerifiedJar;

/**
 * A utility that transforms a BCEL type into its corresponding class tag.
 */
public class BcelToClassImpl implements BcelToClass {

	/**
	 * The class loader for loading the classes during the transformation.
	 */
	private final TakamakaClassLoader classLoader;

	/**
	 * Builds the utility object.
	 * 
	 * @param jar the jar for which the transformation is performed
	 */
	public BcelToClassImpl(VerifiedJar jar) {
		this.classLoader = jar.getClassLoader();
	}

	@Override
	public final Class<?> of(Type type) throws ClassNotFoundException {
		if (type == BasicType.BOOLEAN)
			return boolean.class;
		else if (type == BasicType.BYTE)
			return byte.class;
		else if (type == BasicType.CHAR)
			return char.class;
		else if (type == BasicType.DOUBLE)
			return double.class;
		else if (type == BasicType.FLOAT)
			return float.class;
		else if (type == BasicType.INT)
			return int.class;
		else if (type == BasicType.LONG)
			return long.class;
		else if (type == BasicType.SHORT)
			return short.class;
		else if (type == BasicType.VOID)
			return void.class;
		else if (type instanceof ObjectType)
			return classLoader.loadClass(type.toString());
		else { // array
			Class<?> elementsClass = of(((ArrayType) type).getElementType());
			// we build an array of 0 elements just to access its class token
			return java.lang.reflect.Array.newInstance(elementsClass, 0).getClass();
		}
	}

	@Override
	public final Class<?>[] of(Type[] types) throws ClassNotFoundException {
		Class<?>[] result = new Class<?>[types.length];
		for (int pos = 0; pos < types.length; pos++)
			result[pos] = of(types[pos]);

		return result;
	}
}