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

package io.hotmoka.beans.types.internal;

import java.io.IOException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.types.BasicType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * The basic types of the Takamaka language.
 */
@Immutable
public enum BasicTypes implements BasicType {
	BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}

	@Override
	public int compareAgainst(StorageType other) {
		return other instanceof BasicTypes bt ? compareTo(bt)
			: -1; // other instanceof ClassType
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte((byte) ordinal());
	}

	@Override
	public boolean isEager() {
		return true;
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) ordinal() };
	}

	@Override
	public int size() {
		return 1;
	}
}