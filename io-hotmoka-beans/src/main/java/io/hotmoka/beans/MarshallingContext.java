package io.hotmoka.beans;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.StorageReference;

/**
 * A context used during object marshalling into bytes.
 */
public class MarshallingContext {
	private final ObjectOutputStream oos;
	private final Map<BigInteger, BigInteger> memoryBigInteger = new HashMap<>();
	private final Map<StorageReference, Integer> memoryStorageReference = new HashMap<>();
	private final Map<TransactionReference, Integer> memoryTransactionReference = new HashMap<>();

	public MarshallingContext(ObjectOutputStream oos) {
		this.oos = oos;
	}

	/**
	 * Writes the given string into the output stream. It uses a memory
	 * to avoid repeated writing of the same string: the second write
	 * will refer to the first one.
	 * 
	 * @param s the string to write
	 * @throws IOException if the string could not be written
	 */
	public void writeObject(String s) throws IOException {
		oos.writeObject(s.intern());
	}

	/**
	 * Writes the given big integer into the output stream. It uses a memory
	 * to avoid repeated writing of the same big integer: the second write
	 * will refer to the first one.
	 * 
	 * @param bi the big integer to write
	 * @throws IOException if the big integer could not be written
	 */
	public void writeObject(BigInteger bi) throws IOException {
		oos.writeObject(memoryBigInteger.computeIfAbsent(bi, _bi -> _bi));
	}

	/**
	 * Writes the given storage reference into the output stream. It uses
	 * a memory to recycle storage references already written with this context
	 * and compress them by using their progressive number instead.
	 * 
	 * @param reference the storage reference to write
	 * @throws IOException IOException if the storage reference could not be written
	 */
	public void writeStorageReference(StorageReference reference) throws IOException {
		Integer index = memoryStorageReference.get(reference);
		if (index != null) {
			if (index < 254)
				oos.writeByte(index);
			else {
				oos.writeByte(254);
				oos.writeInt(index);
			}
		}
		else {
			int next = memoryStorageReference.size();
			if (next == Integer.MAX_VALUE) // irrealistic
				throw new InternalFailureException("too many storage references in the same context");

			memoryStorageReference.put(reference, next);

			oos.writeByte(255);
			reference.transaction.into(this);
			writeBigInteger(reference.progressive);
		}
	}

	/**
	 * Writes the given transaction reference into the output stream. It uses
	 * a memory to recycle transaction references already written with this context
	 * and compress them by using their progressive number instead.
	 * 
	 * @param transaction the transaction reference to write
	 * @throws IOException IOException if the transaction reference could not be written
	 */
	public void writeTransactionReference(TransactionReference transaction) throws IOException {
		Integer index = memoryTransactionReference.get(transaction);
		if (index != null) {
			if (index < 254)
				oos.writeByte(index);
			else {
				oos.writeByte(254);
				oos.writeInt(index);
			}
		}
		else {
			int next = memoryTransactionReference.size();
			if (next == Integer.MAX_VALUE) // irrealistic
				throw new InternalFailureException("too many transaction references in the same context");

			memoryTransactionReference.put(transaction, next);

			oos.writeByte(255);
			oos.write(hashAsByteArray(transaction.getHash()));
			//writeSharedByteArray(hashAsByteArray(transaction.getHash()));
		}
	}

	private byte[] hashAsByteArray(String hash) {
		byte[] val = new byte[hash.length() / 2];
		for (int i = 0; i < val.length; i++) {
			int index = i * 2;
			int j = Integer.parseInt(hash.substring(index, index + 2), 16);
			val[i] = (byte) j;
		}

		return val;
	}

	public void writeByte(int b) throws IOException {
		oos.writeByte(b);
	}

	public void writeChar(int c) throws IOException {
		oos.writeChar(c);
	}

	public void writeInt(int i) throws IOException {
		oos.writeInt(i);
	}

	/**
	 * Writes the given integer, in a way that compacts small integers.
	 * 
	 * @param i the integer
	 * @throws IOException if the integer cannot be marshalled
	 */
	public void writeCompactInt(int i) throws IOException {
		if (i < 255)
			writeByte(i);
		else {
			writeByte(255);
			writeInt(i);
		}
	}

	public void writeUTF(String s) throws IOException {
		oos.writeUTF(s);
	}

	public void write(byte[] bytes) throws IOException {
		oos.write(bytes);
	}

	public void writeDouble(double d) throws IOException {
		oos.writeDouble(d);
	}

	public void writeFloat(float f) throws IOException {
		oos.writeFloat(f);
	}

	public void writeLong(long l) throws IOException {
		oos.writeLong(l);
	}

	public void writeShort(int s) throws IOException {
		oos.writeShort(s);
	}

	public void writeBoolean(boolean b) throws IOException {
		oos.writeBoolean(b);
	}

	/**
	 * Writes the given big integer, in a compact way.
	 * 
	 * @param bi the big integer
	 * @throws IOException if the big integer could not be written
	 */
	public void writeBigInteger(BigInteger bi) throws IOException {
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
			byte[] bytes = bi.toByteArray();
			writeCompactInt(bytes.length);
			write(bytes);
		}
	}
}