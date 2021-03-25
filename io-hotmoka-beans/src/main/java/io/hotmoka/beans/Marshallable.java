package io.hotmoka.beans;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
	 * Marshals this object into a given stream. This method in general
	 * performs better than standard Java serialization, wrt the size of the marshalled data.
	 * 
	 * @param context the context holding the stream
	 * @throws IOException if this object cannot be marshalled
	 */
	public abstract void into(MarshallingContext context) throws IOException;

	/**
	 * Marshals an array of marshallables into a given stream.
	 * 
	 * @param marshallables the array of marshallables
	 * @param context the context holding the stream
	 * @throws IOException if some element could not be marshalled
	 */
	public static void intoArray(Marshallable[] marshallables, MarshallingContext context) throws IOException {
		writeCompactInt(marshallables.length, context);

		for (Marshallable marshallable: marshallables)
			marshallable.into(context);
	}

	/**
	 * Marshals an array of marshallables into a given stream.
	 * 
	 * @param marshallables the array of marshallables
	 * @param context the context holding the stream
	 * @throws IOException if some element could not be marshalled
	 */
	public static void intoArrayWithoutSelector(StorageReference[] marshallables, MarshallingContext context) throws IOException {
		writeCompactInt(marshallables.length, context);

		for (StorageReference reference: marshallables)
			reference.intoWithoutSelector(context);
	}

	/**
	 * Marshals the given integer into a given stream.
	 * 
	 * @param i the integer
	 * @param context the context holding the stream
	 * @throws IOException if the integer cannot be marshalled
	 */
	protected static void writeCompactInt(int i, MarshallingContext context) throws IOException {
		if (i < 255)
			context.oos.writeByte(i);
		else {
			context.oos.writeByte(255);
			context.oos.writeInt(i);
		}
	}

	/**
	 * Reads an integer from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the integer
	 * @throws IOException if the integer cannot be unmarshalled
	 */
	protected static int readCompactInt(UnmarshallingContext context) throws IOException {
		int i = context.ois.readByte();
		if (i < 0)
			i += 256;

		if (i == 255)
			i = context.ois.readInt();

		return i;
	}

	/**
	 * Marshals this object into a byte array.
	 * 
	 * @return the byte array resulting from marshalling this object
	 * @throws IOException if this object cannot be marshalled
	 */
	public final byte[] toByteArray() throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			into(new MarshallingContext(oos));
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
			intoArrayWithoutSelector(references, new MarshallingContext(oos));
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
			intoArray(marshallables, new MarshallingContext(oos));
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
		T from(UnmarshallingContext context) throws IOException, ClassNotFoundException;
	}

	/**
	 * Yields an array of marshallables unmarshalled from the given stream.
	 * 
	 * @param <T> the type of the marshallables
	 * @param unmarshaller the object that unmarshals a single marshallable
	 * @param supplier the creator of the resulting array of marshallables
	 * @param context the unmarshalling context
	 * @return the array
	 * @throws IOException if some marshallable could not be unmarshalled
	 * @throws ClassNotFoundException if some marshallable could not be unmarshalled
	 */
	public static <T extends Marshallable> T[] unmarshallingOfArray(Unmarshaller<T> unmarshaller, Function<Integer,T[]> supplier, UnmarshallingContext context) throws IOException, ClassNotFoundException {
		int length = readCompactInt(context);
		T[] result = supplier.apply(length);
		for (int pos = 0; pos < length; pos++)
			result[pos] = unmarshaller.from(context);

		return result;
	}

	/**
	 * Marshals a big integer into a given stream. This method
	 * checks the size of the big integer in order to choose the best
	 * marshalling strategy.
	 * 
	 * @param bi the big integer
	 * @param context the context holding the stream
	 * @throws IOException if the big integer could not be marshalled
	 */
	protected final static void marshal(BigInteger bi, MarshallingContext context) throws IOException {
		short small = bi.shortValue();
		ObjectOutputStream oos = context.oos;

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
			byte[] bytes = bi.toByteArray();
			writeCompactInt(bytes.length, context);
			oos.write(bytes);
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
	protected final static BigInteger unmarshallBigInteger(UnmarshallingContext context) throws ClassNotFoundException, IOException {
		byte selector = context.ois.readByte();
		switch (selector) {
		case 0: return BigInteger.valueOf(context.ois.readShort());
		case 1: return BigInteger.valueOf(context.ois.readInt());
		case 2: return BigInteger.valueOf(context.ois.readLong());
		case 3: {
			int numBytes = readCompactInt(context);
			byte[] bytes = new byte[numBytes];
			if (numBytes != context.ois.readNBytes(bytes, 0, numBytes))
				throw new IOException("BigInteger length mismatch");

			return new BigInteger(bytes);
		}
		default: {
			if (selector - 4 < 0)
				return BigInteger.valueOf(selector + 252);
			else
				return BigInteger.valueOf(selector - 4);
		}
		}
	}
}