package io.takamaka.code.engine;

/**
 * A store that can be checked out, that is, its view of the world can be moved
 * back in time. Different moments of the store are identifies by hashes, that
 * can be checked out when needed.
 */
public interface CheckableStore extends Store {

	/**
	 * Resets the store to the view of the world expressed by the given hash.
	 * 
	 * @param hash the hash to reset to
	 */
	void checkout(byte[] hash);
}