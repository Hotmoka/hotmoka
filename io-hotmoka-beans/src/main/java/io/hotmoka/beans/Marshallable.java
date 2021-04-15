package io.hotmoka.beans;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); MarshallingContext context = new MarshallingContext(baos)) {
			into(context);
			context.flush();
			return baos.toByteArray();
		}
	}

	/**
	 * Marshals an array of marshallables into a byte array.
	 * 
	 * @return the byte array resulting from marshalling the array of marshallables
	 * @throws IOException if some marshallable could not be marshalled
	 */
	public static byte[] toByteArray(Marshallable[] marshallables) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); MarshallingContext context = new MarshallingContext(baos)) {
			intoArray(marshallables, context);
			context.flush();
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
}