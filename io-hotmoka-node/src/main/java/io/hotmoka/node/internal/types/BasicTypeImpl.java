/*
Copyright 2023 Fausto Spoto

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
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.api.types.BasicType;
import io.hotmoka.node.api.types.StorageType;

/**
 * The basic types of the Takamaka language.
 */
@Immutable
public class BasicTypeImpl extends AbstractStorageType implements BasicType {

	/**
	 * The {@code boolean} basic type of the Takamaka language.
	 */
	public final static BasicType BOOLEAN = new BasicTypeImpl();

	/**
	 * The {@code byte} basic type of the Takamaka language.
	 */
	public final static BasicType BYTE = new BasicTypeImpl();

	/**
	 * The {@code char} basic type of the Takamaka language.
	 */
	public final static BasicType CHAR = new BasicTypeImpl();

	/**
	 * The {@code short} basic type of the Takamaka language.
	 */
	public final static BasicType SHORT = new BasicTypeImpl();

	/**
	 * The {@code int} basic type of the Takamaka language.
	 */
	public final static BasicType INT = new BasicTypeImpl();

	/**
	 * The {@code long} basic type of the Takamaka language.
	 */
	public final static BasicType LONG = new BasicTypeImpl();

	/**
	 * The {@code float} basic type of the Takamaka language.
	 */
	public final static BasicType FLOAT = new BasicTypeImpl();
	
	/**
	 * The {@code double} basic type of the Takamaka language.
	 */
	public final static BasicType DOUBLE = new BasicTypeImpl();

	private BasicTypeImpl() {}

	/**
	 * Yields the basic type with the given selector, if any.
	 * 
	 * @param selector the selector
	 * @return the basic type; this is {@code null} if no basic type uses the given selector
	 */
	static BasicType withSelector(byte selector) {
		switch (selector) {
		case 0:
			return BOOLEAN;
		case 1:
			return BYTE;
		case 2:
			return CHAR;
		case 3:
			return SHORT;
		case 4:
			return INT;
		case 5:
			return LONG;
		case 6:
			return FLOAT;
		case 7:
			return DOUBLE;
		default:
			return null;
		}
	}

	@Override
	public byte ordinal() {
		if (this == BOOLEAN)
			return 0;
		else if (this == BYTE)
			return 1;
		else if (this == CHAR)
			return 2;
		else if (this == SHORT)
			return 3;
		else if (this == INT)
			return 4;
		else if (this == LONG)
			return 5;
		else if (this == FLOAT)
			return 6;
		else // if (this == DOUBLE)
			return 7;
	}

	@Override
	public String toString() {
		if (this == BOOLEAN)
			return "boolean";
		else if (this == BYTE)
			return "byte";
		else if (this == CHAR)
			return "char";
		else if (this == SHORT)
			return "short";
		else if (this == INT)
			return "int";
		else if (this == LONG)
			return "long";
		else if (this == FLOAT)
			return "float";
		else // if (this == DOUBLE)
			return "double";
	}

	@Override
	public int compareTo(StorageType other) {
		return other instanceof BasicType bt ? ordinal() - bt.ordinal()
			: -1; // other instanceof ClassType
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(ordinal());
	}

	@Override
	public boolean isEager() {
		return true;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BasicType bt && ordinal() == bt.ordinal();
	}

	@Override
	public int hashCode() {
		return ordinal();
	}
}