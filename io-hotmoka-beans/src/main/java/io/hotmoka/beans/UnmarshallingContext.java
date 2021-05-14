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

package io.hotmoka.beans;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.hotmoka.beans.Marshallable.Unmarshaller;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.StorageReference;

/**
 * A context used during bytes unmarshalling into objects.
 */
public class UnmarshallingContext implements AutoCloseable {
	private final ObjectInputStream ois;
	private final Map<Integer, StorageReference> memoryStorageReference = new HashMap<>();
	private final Map<Integer, TransactionReference> memoryTransactionReference = new HashMap<>();
	private final Map<Integer, String> memoryString = new HashMap<>();
	private final Map<Integer, FieldSignature> memoryFieldSignature = new HashMap<>();

	public UnmarshallingContext(InputStream is) throws IOException {
		this.ois = new ObjectInputStream(new BufferedInputStream(is));
	}

	/**
	 * Yields an array of marshallables unmarshalled from this context.
	 * 
	 * @param <T> the type of the marshallables
	 * @param unmarshaller the object that unmarshals a single marshallable
	 * @param supplier the creator of the resulting array of marshallables
	 * @return the array
	 * @throws IOException if some marshallable could not be unmarshalled
	 * @throws ClassNotFoundException if some marshallable could not be unmarshalled
	 */
	public <T extends Marshallable> T[] readArray(Unmarshaller<T> unmarshaller, Function<Integer,T[]> supplier) throws IOException, ClassNotFoundException {
		int length = readCompactInt();
		T[] result = supplier.apply(length);
		for (int pos = 0; pos < length; pos++)
			result[pos] = unmarshaller.from(this);

		return result;
	}

	/**
	 * Reads a storage reference from this context. It uses progressive counters to
	 * decompress repeated storage references for the same context.
	 * 
	 * @return the storage reference
	 */
	public StorageReference readStorageReference() throws ClassNotFoundException, IOException {
		int selector = ois.readByte();
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			StorageReference reference = new StorageReference(TransactionReference.from(this), readBigInteger());
			memoryStorageReference.put(memoryStorageReference.size(), reference);
			return reference;
		}
		else if (selector == 254)
			return memoryStorageReference.get(ois.readInt());
		else
			return memoryStorageReference.get(selector);
	}

	/**
	 * Reads a field signature from this context. It uses progressive counters to
	 * decompress repeated field signatures for the same context.
	 * 
	 * @return the field signature
	 */
	public FieldSignature readFieldSignature() throws IOException {
		int selector = ois.readByte();
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			FieldSignature field = new FieldSignature((ClassType) StorageType.from(this), readUTF(), StorageType.from(this));
			memoryFieldSignature.put(memoryFieldSignature.size(), field);
			return field;
		}
		else if (selector == 254)
			return memoryFieldSignature.get(ois.readInt());
		else
			return memoryFieldSignature.get(selector);
	}

	/**
	 * Reads a transaction reference from this context. It uses progressive counters to
	 * decompress repeated transaction references for the same context.
	 * 
	 * @return the transaction reference
	 */
	public TransactionReference readTransactionReference() throws IOException {
		int selector = ois.readByte();
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			byte[] bytes = ois.readNBytes(TransactionRequest.REQUEST_HASH_LENGTH);
			TransactionReference reference = new LocalTransactionReference(bytes);
			memoryTransactionReference.put(memoryTransactionReference.size(), reference);
			return reference;
		}
		else if (selector == 254)
			return memoryTransactionReference.get(ois.readInt());
		else
			return memoryTransactionReference.get(selector);
	}

	public byte readByte() throws IOException {
		return ois.readByte();
	}

	public char readChar() throws IOException {
		return ois.readChar();
	}

	public boolean readBoolean() throws IOException {
		return ois.readBoolean();
	}

	public int readInt() throws IOException {
		return ois.readInt();
	}

	/**
	 * Reads a small integer.
	 * 
	 * @return the integer
	 * @throws IOException if the integer cannot be written
	 */
	public int readCompactInt() throws IOException {
		int i = readByte();
		if (i < 0)
			i += 256;

		if (i == 255)
			i = readInt();

		return i;
	}

	public short readShort() throws IOException {
		return ois.readShort();
	}

	public long readLong() throws IOException {
		return ois.readLong();
	}

	public float readFloat() throws IOException {
		return ois.readFloat();
	}

	public double readDouble() throws IOException {
		return ois.readDouble();
	}

	public String readUTF() throws IOException {
		return ois.readUTF();
	}

	public byte[] readBytes(int length, String errorMessage) throws IOException {
		byte[] bytes = new byte[length];
		if (length != ois.readNBytes(bytes, 0, length))
			throw new IOException(errorMessage);

		return bytes;
	}

	public String readStringShared() throws IOException {
		int selector = ois.readByte();
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			String s = ois.readUTF();
			memoryString.put(memoryString.size(), s);
			return s;
		}
		else if (selector == 254)
			return memoryString.get(ois.readInt());
		else
			return memoryString.get(selector);
	}

	/**
	 * Reads a big integer, taking into account
	 * optimized representations used for the big integer.
	 * 
	 * @return the big integer
	 * @throws IOException if the big integer could not be written
	 */
	public BigInteger readBigInteger() throws IOException {
		byte selector = readByte();
		switch (selector) {
		case 0: return BigInteger.valueOf(readShort());
		case 1: return BigInteger.valueOf(readInt());
		case 2: return BigInteger.valueOf(readLong());
		case 3: {
			int numBytes = readCompactInt();
			return new BigInteger(readBytes(numBytes, "BigInteger length mismatch"));
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