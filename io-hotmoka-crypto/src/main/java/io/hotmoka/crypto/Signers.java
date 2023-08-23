package io.hotmoka.crypto;

import java.security.KeyPair;
import java.security.PrivateKey;

import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;

/**
 * Providers of objects that sign values with a private key.
 */
public final class Signers<T> {

	private Signers() {}

	/**
	 * Yields a signer for the given algorithm with the given key pair.
	 * 
	 * @param signature the signing algorithm
	 * @param keys the key pair
	 * @return the signer
	 */
	public static <T> Signer<T> with(SignatureAlgorithm<? super T> signature, KeyPair keys) {
		return with(signature, keys.getPrivate());
	}

	/**
	 * Yields a signer for the given algorithm with the given private key.
	 * 
	 * @param signature the signing algorithm
	 * @param key the private key
	 * @return the signer
	 */
	public static <T> Signer<T> with(SignatureAlgorithm<? super T> signature, PrivateKey key) {
		return what -> signature.sign(what, key);
	}
}