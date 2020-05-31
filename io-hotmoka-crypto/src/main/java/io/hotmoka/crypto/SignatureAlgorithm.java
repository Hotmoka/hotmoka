package io.hotmoka.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

import io.hotmoka.crypto.internal.SHA256withDSA;

/**
 * An algorithm that signs values and verifies such signatures back.
 *
 * @param <T> the type of values that get signed
 */
public interface SignatureAlgorithm<T> {

	/**
	 * Yields the signature of the given value, by using the given private key.
	 * 
	 * @param what the value to sign
	 * @param privateKey the private key used for signing
	 * @return the sequence of bytes
	 * @throws InvalidKeyException if the provided private key is invalid
	 * @throws SignatureException if the value cannot be signed
	 */
	byte[] sign(T what, PrivateKey privateKey) throws InvalidKeyException, SignatureException;

	/**
	 * Verifies that the given signature corresponds to the given value, by using
	 * the given public key.
	 * 
	 * @param what the value whose signature gets verified
	 * @param publicKey the public key; its corresponding private key should have been used for signing
	 * @param signature the signature to verify
	 * @return true if and only if the signature matches
	 * @throws InvalidKeyException if the provided public key is invalid
	 * @throws SignatureException if the value cannot be signed
	 */
	boolean verify(T what, PublicKey publicKey, byte[] signature) throws InvalidKeyException, SignatureException;

	/**
	 * Yields a signature algorithm that uses the SHA256 hashing algorithm and then the DSA algorithm.
	 * 
	 * @param <T> the type of values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation of Java does not include the SHA256withDSA algorithm
	 */
	static <T> SignatureAlgorithm<T> sha256dsa(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		return new SHA256withDSA<>(supplier);
	}
}