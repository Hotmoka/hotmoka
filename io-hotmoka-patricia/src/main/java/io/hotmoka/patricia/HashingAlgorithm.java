package io.hotmoka.patricia;

/**
 * An algorithm that hashes values into bytes.
 *
 * @param <T> the type of the values that get hashed.
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
}