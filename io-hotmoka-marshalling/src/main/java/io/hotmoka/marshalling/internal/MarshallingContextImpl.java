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

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.exceptions.UncheckedIOException;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.ObjectMarshaller;

/**
 * Implementation of a context used during object marshaling into bytes.
 */
public class MarshallingContextImpl implements MarshallingContext {
	private final ObjectOutputStream oos;
	private final Map<String, Integer> memoryString = new HashMap<>();

	/**
	 * Object marshallers for specific classes, if any.
	 */
	private final Map<Class<?>, ObjectMarshaller<?>> objectMarshallers = new HashMap<>();

	public MarshallingContextImpl(OutputStream oos) {
		this.oos = UncheckedIOException.wraps(() -> new ObjectOutputStream(oos));
	}

	/**
	 * Registers an object marshaller. It will be used to marshall its class.
	 * 
	 * @param om the object marshaller
	 */
	protected void registerObjectMarshaller(ObjectMarshaller<?> om) {
		objectMarshallers.put(om.clazz(), om);
	}

	@Override
	public <C> void writeObject(Class<C> clazz, C value) {
		@SuppressWarnings("unchecked")
		var om = (ObjectMarshaller<C>) objectMarshallers.get(clazz);
		if (om == null)
			throw new IllegalStateException("missing object marshaller for class " + clazz.getName());

		om.write(value, this);
	}

	@Override
	public void writeStringShared(String s) {
		Integer index = memoryString.get(s);

		UncheckedIOException.wraps(() -> {
			if (index != null) {
				if (index < 254)
					oos.writeByte(index);
				else {
					oos.writeByte(254);
					oos.writeInt(index);
				}
			}
			else {
				int next = memoryString.size();
				if (next == Integer.MAX_VALUE) // irrealistic
					throw new IllegalStateException("too many strings in the same context");

				memoryString.put(s, next);

				oos.writeByte(255);
				oos.writeUTF(s);
			}
		});
	}

	@Override
	public void writeByte(int b) {
		UncheckedIOException.wraps(() -> oos.writeByte(b));
	}

	@Override
	public void writeChar(int c) {
		UncheckedIOException.wraps(() -> oos.writeChar(c));
	}

	@Override
	public void writeInt(int i) {
		UncheckedIOException.wraps(() -> oos.writeInt(i));
	}

	@Override
	public void writeCompactInt(int i) {
		if (i < 255)
			writeByte(i);
		else {
			writeByte(255);
			writeInt(i);
		}
	}

	@Override
	public void writeUTF(String s) {
		UncheckedIOException.wraps(() -> oos.writeUTF(s));
	}

	@Override
	public void write(byte[] bytes) {
		UncheckedIOException.wraps(() -> oos.write(bytes));
	}

	@Override
	public void writeDouble(double d) {
		UncheckedIOException.wraps(() -> oos.writeDouble(d));
	}

	@Override
	public void writeFloat(float f) {
		UncheckedIOException.wraps(() -> oos.writeFloat(f));
	}

	@Override
	public void writeLong(long l) {
		UncheckedIOException.wraps(() -> oos.writeLong(l));
	}

	@Override
	public void writeShort(int s) {
		UncheckedIOException.wraps(() -> oos.writeShort(s));
	}

	@Override
	public void writeBoolean(boolean b) {
		UncheckedIOException.wraps(() -> oos.writeBoolean(b));
	}

	@Override
	public void writeBigInteger(BigInteger bi) {
		short small = bi.shortValue();

		if (BigInteger.valueOf(small).equals(bi)) {
			if (0 <= small && small <= 251)
				writeByte(4 + small);
			else {
				writeByte(0);
				writeShort(small);
			}
		}
		else if (BigInteger.valueOf(bi.intValue()).equals(bi)) {
			writeByte(1);
			writeInt(bi.intValue());
		}
		else if (BigInteger.valueOf(bi.longValue()).equals(bi)) {
			writeByte(2);
			writeLong(bi.longValue());
		}
		else {
			writeByte(3);
			byte[] bytes = bi.toString().getBytes();
			writeCompactInt(bytes.length);
			write(bytes);
		}
	}

	@Override
	public void flush() {
		UncheckedIOException.wraps(oos::flush);
	}

	@Override
	public void close() {
		UncheckedIOException.wraps(oos::close);
	}
}