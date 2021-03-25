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
	public final ObjectOutputStream oos;
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
	 * Writes the given array of bytes into the output stream. It uses a memory
	 * to avoid repeated writing of the same array: the second write
	 * will refer to the first one.
	 * 
	 * @param bytes the array of bytes
	 * @throws IOException if the array could not be written
	 */
	/*private void writeSharedByteArray(byte[] bytes) throws IOException {
		oos.writeObject(memoryArrays.computeIfAbsent(new ByteArray(bytes), _ba -> _ba.bytes));
	}*/

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
			Marshallable.marshal(reference.progressive, this);
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
}