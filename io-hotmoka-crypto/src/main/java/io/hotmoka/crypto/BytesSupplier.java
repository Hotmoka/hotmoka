package io.hotmoka.crypto;

/**
 * A function that maps objects into a byte sequence.
 *
 * @param <T> the type of the objects
 */
public interface BytesSupplier<T> {

	/**
	 * Maps an object into a byte sequence.
	 * 
	 * @param what the object
	 * @return the byte sequence
	 * @throws Exception if an error occurs during the mapping
	 */
	byte[] get(T what) throws Exception;
}