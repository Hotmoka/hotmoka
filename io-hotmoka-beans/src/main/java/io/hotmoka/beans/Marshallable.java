package io.hotmoka.beans;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.function.Function;

import io.hotmoka.beans.values.StorageReference;

/**
 * An object that can be marshalled into a stream, in a way
 * more compact than standard Java serialization. TYpically,
 * this works because of context information about the structure
 * of the object.
 */
public abstract class Marshallable {

	/**
	 * Marshals this object into the given stream. This method in general
	 * performs better than standard Java serialization, wrt the size of the marshalled data.
	 * 
	 * @param oos the stream
	 * @throws IOException if this object cannot be marshalled
	 */
	public abstract void into(ObjectOutputStream oos) throws IOException;

	/**
	 * Marshals an array of marshallables into the given stream.
	 * 
	 * @param marshallables the array of marshallables
	 * @param oos the stream
	 * @throws IOException if some element could not be marshalled
	 */
	public static void intoArray(Marshallable[] marshallables, ObjectOutputStream oos) throws IOException {
		writeLength(marshallables.length, oos);

		for (Marshallable marshallable: marshallables)
			marshallable.into(oos);
	}

	/**
	 * Marshals an array of marshallables into the given stream.
	 * 
	 * @param marshallables the array of marshallables
	 * @param oos the stream
	 * @throws IOException if some element could not be marshalled
	 */
	public static void intoArrayWithoutSelector(StorageReference[] marshallables, ObjectOutputStream oos) throws IOException {
		writeLength(marshallables.length, oos);

		for (StorageReference reference: marshallables)
			reference.intoWithoutSelector(oos);
	}

	/**
	 * Marshals the given length into the given stream.
	 * 
	 * @param length the length
	 * @param oos the stream
	 * @throws IOException if the length cannot be marshalled
	 */
	protected static void writeLength(int length, ObjectOutputStream oos) throws IOException {
		if (length < 255)
			oos.writeByte(length);
		else {
			oos.writeByte(255);
			oos.writeInt(length);
		}
	}

	/**
	 * Reads a length from the given stream.
	 * 
	 * @param ois the stream
	 * @return the length
	 * @throws IOException if the length cannot be unmarshalled
	 */
	protected static int readLength(ObjectInputStream ois) throws IOException {
		int length = ois.readByte();
		if (length < 0)
			length += 256;

		if (length == 255)
			length = ois.readInt();

		return length;
	}

	/**
	 * Marshals this object into a byte array.
	 * 
	 * @return the byte array resulting from marshalling this object
	 * @throws IOException if this object cannot be marshalled
	 */
	public final byte[] toByteArray() throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			into(oos);
			oos.flush();
			return baos.toByteArray();
		}
	}

	/**
	 * Marshals an array of storage references into a byte array.
	 * 
	 * @return the byte array resulting from marshalling the array of storage references
	 * @throws IOException if some storage reference could not be marshalled
	 */
	public final static byte[] toByteArrayWithoutSelector(StorageReference[] references) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			intoArrayWithoutSelector(references, oos);
			oos.flush();
			return baos.toByteArray();
		}
	}

	/**
	 * Marshals an array of marshallables into a byte array.
	 * 
	 * @return the byte array resulting from marshalling the array of marshallables
	 * @throws IOException if some marshallable could not be marshalled
	 */
	public final static byte[] toByteArray(Marshallable[] marshallables) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			intoArray(marshallables, oos);
			oos.flush();
			return baos.toByteArray();
		}
	}

	/**
	 * A function that unmarshals a single marshallable.
	 *
	 * @param <T> the type of the marshallable
	 */
	public interface Unmarshaller<T extends Marshallable> {
		T from(ObjectInputStream ois) throws IOException, ClassNotFoundException;
	}

	/**
	 * Yields an array of marshallables unmarshalled from the given stream.
	 * 
	 * @param <T> the type of the marshallables
	 * @param unmarshaller the object that unmarshals a single marshallable
	 * @param supplier the creator of the resulting array of marshallables
	 * @param ois the stream
	 * @return the array
	 * @throws IOException if some marshallable could not be unmarshalled
	 * @throws ClassNotFoundException if some marshallable could not be unmarshalled
	 */
	public static <T extends Marshallable> T[] unmarshallingOfArray(Unmarshaller<T> unmarshaller, Function<Integer,T[]> supplier, ObjectInputStream ois) throws IOException, ClassNotFoundException {
		int length = readLength(ois);
		T[] result = supplier.apply(length);
		for (int pos = 0; pos < length; pos++)
			result[pos] = unmarshaller.from(ois);

		return result;
	}

	/**
	 * Marshals a big integer into the given stream. This method
	 * checks the size of the big integer in order to choose the best
	 * marshalling strategy.
	 * 
	 * @param bi the big integer
	 * @param oos the stream
	 * @throws IOException if the big integer could not be marshalled
	 */
	protected final static void marshal(BigInteger bi, ObjectOutputStream oos) throws IOException {
		short small = bi.shortValue();
		if (BigInteger.valueOf(small).equals(bi)) {
			if (0 <= small && small <= 251)
				oos.writeByte(4 + small);
			else {
				oos.writeByte(0);
				oos.writeShort(small);
			}
		}
		else if (BigInteger.valueOf(bi.intValue()).equals(bi)) {
			oos.writeByte(1);
			oos.writeInt(bi.intValue());
		}
		else if (BigInteger.valueOf(bi.longValue()).equals(bi)) {
			oos.writeByte(2);
			oos.writeLong(bi.longValue());
		}
		else {
			oos.writeByte(3);
			oos.writeObject(bi);
		}
	}

	/**
	 * Unmarshals a big integer from the given stream, taking into account
	 * optimized representations used for the big integer.
	 * 
	 * @param ois the stream
	 * @return the big integer
	 * @throws ClassNotFoundException if the big integer could not be unmarshalled
	 * @throws IOException if the big integer could not be unmarshalled
	 */
	protected final static BigInteger unmarshallBigInteger(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		byte selector = ois.readByte();
		switch (selector) {
		case 0: return BigInteger.valueOf(ois.readShort());
		case 1: return BigInteger.valueOf(ois.readInt());
		case 2: return BigInteger.valueOf(ois.readLong());
		case 3: return (BigInteger) ois.readObject();
		default: {
			if (selector - 4 < 0)
				return BigInteger.valueOf(selector + 252);
			else
				return BigInteger.valueOf(selector - 4);
		}
		}
	}
}