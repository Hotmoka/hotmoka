package io.hotmoka.crypto;

import java.security.NoSuchAlgorithmException;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.crypto.internal.SHA256;

/**
 * An algorithm that hashes values into bytes.
 *
 * @param <T> the type of values that get hashed.
 */
public interface HashingAlgorithm<T> {

	/**
	 * Hashes the given value into a sequence of bytes.
	 * 
	 * @param what the value to hash
	 * @return the sequence of bytes; this must have length equals to {@linkplain #length()}
	 */
	byte[] hash(T what);

	/**
	 * The length of the sequence of bytes resulting from hashing a value.
	 * This length must be constant, independent from the specific value that gets hashed.
	 *
	 * @return the length
	 */
	int length();

	/**
	 * Yields a hashing algorithm that uses the SHA256 hashing algorithm.
	 * 
	 * @param <T> the type of values that get hashed
	 * @param supplier how values get transformed into bytes, before being hashed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation of Java does not include the SHA256 algorithm
	 */
	static <T> HashingAlgorithm<T> sha256(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		return new SHA256<>(supplier);
	}

	/**
	 * Yields a hashing algorithm for marshallable values, that uses the SHA256 hashing algorithm.
	 * Values are transformed into bytes by using their {@linkplain Marshallable#toByteArray()} method.
	 * 
	 * @param <T> the type of values that get hashed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation of Java does not include the SHA256 algorithm
	 */
	static <T extends Marshallable> HashingAlgorithm<T> sha256() throws NoSuchAlgorithmException {
		return new SHA256<>(Marshallable::toByteArray);
	}
}