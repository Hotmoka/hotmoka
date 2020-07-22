package io.hotmoka.crypto;

/**
 * A class with the same name as a Hotmoka class. Once installed
 * in a node, a call should lead here, not inside its implementation
 * of the node!
 *
 * @param <T> the type of values that get signed
 */
public interface SignatureAlgorithm<T> {

	/**
	 * Yields an empty signature algorithm that signs everything with an empty array of bytes.
	 * 
	 * @param <T> the type of values that get signed
	 * @return the algorithm
	 */
	static <T> SignatureAlgorithm<T> empty() {
		// the real implementation does not return null; this will allow our test case
		// to understand that this implementation has been called
		return null;
	}
}