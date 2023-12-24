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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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
	 * @throws IOException if the context cannot be created
	 */
	public UnmarshallingContextImpl(InputStream is) throws IOException {
		this.ois = new ObjectInputStream(new BufferedInputStream(is));
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
	public <C> C readObject(Class<C> clazz) throws IOException {
		@SuppressWarnings("unchecked")
		var ou = (ObjectUnmarshaller<C>) objectUnmarshallers.get(clazz);
		Objects.requireNonNull(ou, "Missing object unmarshaller for class " + clazz.getName());

		return ou.read(this);
	}

	@Override
	public <T extends Marshallable> T[] readLengthAndArray(Unmarshaller<T> unmarshaller, Function<Integer,T[]> supplier) throws IOException {
		int length = readCompactInt();
		T[] result = supplier.apply(length);
		for (int pos = 0; pos < length; pos++)
			result[pos] = unmarshaller.from(this);

		return result;
	}

	@Override
	public byte[] readLengthAndBytes(String mismatchErrorMessage) throws IOException {
		return readBytes(readCompactInt(), mismatchErrorMessage);
	}

	@Override
	public byte readByte() throws IOException {
		return ois.readByte();
	}

	@Override
	public char readChar() throws IOException {
		return ois.readChar();
	}

	@Override
	public boolean readBoolean() throws IOException {
		return ois.readBoolean();
	}

	@Override
	public int readInt() throws IOException {
		return ois.readInt();
	}

	@Override
	public int readCompactInt() throws IOException {
		int i = readByte();
		if (i < 0)
			i += 256;

		switch (i) {
		case 255: return readInt();
		case 254: return readShort();
		default: return i;
		}
	}

	@Override
	public short readShort() throws IOException {
		return ois.readShort();
	}

	@Override
	public long readLong() throws IOException {
		return ois.readLong();
	}

	@Override
	public long readCompactLong() throws IOException {
		int i = readByte();
		if (i < 0)
			i += 256;

		switch (i) {
		case 255: return readLong();
		case 254: return readInt();
		case 253: return readShort();
		default: return i;
		}
	}

	@Override
	public float readFloat() throws IOException {
		return ois.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return ois.readDouble();
	}

	@Override
	public String readStringUnshared() throws IOException {
		return ois.readUTF();
	}

	@Override
	public byte[] readBytes(int length, String mismatchErrorMessage) throws IOException {
		var bytes = new byte[length];
		if (length != ois.readNBytes(bytes, 0, length))
			throw new IOException(mismatchErrorMessage);

		return bytes;
	}

	@Override
	public String readStringShared() throws IOException {
		int selector = readByte();
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			String s = readStringUnshared();
			memoryString.put(memoryString.size(), s);
			return s;
		}
		else if (selector == 254)
			return memoryString.get(readInt());
		else
			return memoryString.get(selector);
	}

	@Override
	public BigInteger readBigInteger() throws IOException {
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
	public void close() throws IOException {
		ois.close();
	}
}