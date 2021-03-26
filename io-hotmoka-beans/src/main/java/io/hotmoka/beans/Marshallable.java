package io.hotmoka.beans;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
		context.writeCompactInt(marshallables.length);

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
		context.writeCompactInt(marshallables.length);

		for (StorageReference reference: marshallables)
			reference.intoWithoutSelector(context);
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
		int length = context.readCompactInt();
		T[] result = supplier.apply(length);
		for (int pos = 0; pos < length; pos++)
			result[pos] = unmarshaller.from(context);

		return result;
	}
}