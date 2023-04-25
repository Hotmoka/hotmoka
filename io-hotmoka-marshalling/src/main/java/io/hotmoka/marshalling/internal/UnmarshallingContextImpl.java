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

package io.hotmoka.marshalling.internal;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.hotmoka.exceptions.UncheckedIOException;
import io.hotmoka.marshalling.api.Marshallable;
import io.hotmoka.marshalling.api.ObjectUnmarshaller;
import io.hotmoka.marshalling.api.Unmarshaller;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Implementation of a context used during bytes unmarshalling into objects.
 */
public class UnmarshallingContextImpl implements UnmarshallingContext {
	private final ObjectInputStream ois;

	/**
	 * A memory to avoid duplicated strings in the marshalled bytes.
	 */
	private final Map<Integer, String> memoryString = new HashMap<>();

	/**
	 * Object marshallers for specific classes, if any.
	 */
	private final Map<Class<?>, ObjectUnmarshaller<?>> objectUnmarshallers = new HashMap<>();

	/**
	 * Creates an unmarshalling context.
	 * 
	 * @param is the input stream of the context
	 * @throws UncheckedIOException if the context cannot be created
	 */
	public UnmarshallingContextImpl(InputStream is) {
		this.ois = UncheckedIOException.wraps(() -> new ObjectInputStream(new BufferedInputStream(is)));
	}

	/**
	 * Registers an object unmarshaller. It will be used to unmarshall its class.
	 * 
	 * @param om the object unmarhaller
	 */
	protected void registerObjectUnmarshaller(ObjectUnmarshaller<?> ou) {
		objectUnmarshallers.put(ou.clazz(), ou);
	}

	@Override
	public <C> C readObject(Class<C> clazz) {
		@SuppressWarnings("unchecked")
		var ou = (ObjectUnmarshaller<C>) objectUnmarshallers.get(clazz);
		if (ou == null)
			throw new IllegalStateException("missing object unmarshaller for class " + clazz.getName());

		return ou.read(this);
	}

	@Override
	public <T extends Marshallable> T[] readArray(Unmarshaller<T> unmarshaller, Function<Integer,T[]> supplier) {
		int length = readCompactInt();
		T[] result = supplier.apply(length);
		for (int pos = 0; pos < length; pos++)
			result[pos] = unmarshaller.from(this);

		return result;
	}

	@Override
	public byte readByte() {
		return UncheckedIOException.wraps(ois::readByte);
	}

	@Override
	public char readChar() {
		return UncheckedIOException.wraps(ois::readChar);
	}

	@Override
	public boolean readBoolean() {
		return UncheckedIOException.wraps(ois::readBoolean);
	}

	@Override
	public int readInt() {
		return UncheckedIOException.wraps(ois::readInt);
	}

	@Override
	public int readCompactInt() {
		int i = readByte();
		if (i < 0)
			i += 256;

		if (i == 255)
			i = readInt();

		return i;
	}

	@Override
	public short readShort() {
		return UncheckedIOException.wraps(ois::readShort);
	}

	@Override
	public long readLong() {
		return UncheckedIOException.wraps(ois::readLong);
	}

	@Override
	public float readFloat() {
		return UncheckedIOException.wraps(ois::readFloat);
	}

	@Override
	public double readDouble() {
		return UncheckedIOException.wraps(ois::readDouble);
	}

	@Override
	public String readUTF() {
		return UncheckedIOException.wraps(ois::readUTF);
	}

	@Override
	public byte[] readBytes(int length, String errorMessage) {
		byte[] bytes = new byte[length];
		if (length != UncheckedIOException.wraps(() -> ois.readNBytes(bytes, 0, length)))
			throw new UncheckedIOException(errorMessage);

		return bytes;
	}

	@Override
	public String readStringShared() {
		int selector = readByte();
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			String s = readUTF();
			memoryString.put(memoryString.size(), s);
			return s;
		}
		else if (selector == 254)
			return memoryString.get(readInt());
		else
			return memoryString.get(selector);
	}

	@Override
	public BigInteger readBigInteger() {
		byte selector = readByte();
		switch (selector) {
		case 0: return BigInteger.valueOf(readShort());
		case 1: return BigInteger.valueOf(readInt());
		case 2: return BigInteger.valueOf(readLong());
		case 3: {
			int numBytes = readCompactInt();
			return new BigInteger(new String(readBytes(numBytes, "BigInteger length mismatch")));
		}
		default: {
			if (selector - 4 < 0)
				return BigInteger.valueOf(selector + 252);
			else
				return BigInteger.valueOf(selector - 4);
		}
		}
	}

	@Override
	public void close() {
		UncheckedIOException.wraps(ois::close);
	}
}