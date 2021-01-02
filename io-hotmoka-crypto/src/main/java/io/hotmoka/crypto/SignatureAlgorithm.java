package io.hotmoka.crypto;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import io.hotmoka.crypto.internal.*;

/**
 * An algorithm that signs values and verifies such signatures back.
 *
 * @param <T> the type of values that get signed
 */
public interface SignatureAlgorithm<T> {

	/**
	 * Yields a pair of keys (private/public) that can be used with
	 * this signature algorithm.
	 * 
	 * @return the pair of keys
	 */
	KeyPair getKeyPair();

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
	 * Yields a public key from that can be used with this signature, from
	 * its encoded version as a byte array.
	 * 
	 * @param encoded the encoded version of the public key
	 * @return the public key
	 * @throws NoSuchAlgorithmException if the key algorithm does not exist in the Java installation
	 * @throws NoSuchProviderException if the key provider does not exist in the Java installation
	 * @throws InvalidKeySpecException if the {@code encoded} key does not match the expected specification
	 */
	PublicKey publicKeyFromEncoded(byte[] encoded) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException;

	/**
	 * Yields a signature algorithm that uses the SHA256 hashing algorithm and then the DSA algorithm.
	 * 
	 * @param <T> the type of values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation of Java does not include the SHA256withDSA algorithm
	 */
	static <T> SignatureAlgorithm<T> sha256dsa(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		return new SHA256DSA<>(supplier);
	}

	/**
	 * Yields the ed25519 signature algorithm.
	 * 
	 * @param <T> the type of values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the ed25519 algorithm
	 */
	static <T> SignatureAlgorithm<T> ed25519(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		return new ED25519<>(supplier);
	}

	/**
	 * Yields the qTESLA-p-I signature algorithm.
	 *
	 * @param <T> the type of values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the qTESLA-p-I algorithm
	 */
	static <T> SignatureAlgorithm<T> qtesla1(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		return new QTESLA1<>(supplier);
	}

	/**
	 * Yields the qTESLA-p-III signature algorithm.
	 *
	 * @param <T> the type of values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the qTESLA-p-III algorithm
	 */
	static <T> SignatureAlgorithm<T> qtesla3(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		return new QTESLA3<>(supplier);
	}

	/**
	 * Yields an empty signature algorithm that signs everything with an empty array of bytes.
	 * 
	 * @param <T> the type of values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed;
	 *                 this is not actually used by this algorithm
	 * @return the algorithm
	 */
	static <T> SignatureAlgorithm<T> empty(BytesSupplier<? super T> supplier) {
		return new EMPTY<>();
	}

	/**
	 * Yields the signature algorithm with the given name.
	 * It looks for a factory method with the given name and invokes it.
	 * 
	 * @param <T> the type of the values that get signed
	 * @param name the name of the algorithm, case-insensitive
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the given algorithm
	 */
	@SuppressWarnings("unchecked")
	static <T> SignatureAlgorithm<T> mk(String name, BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		name = name.toLowerCase();

		try {
			// only sha256dsa, ed25519, empty and qtesla are currently found below
			Method method = SignatureAlgorithm.class.getMethod(name, BytesSupplier.class);
			return (SignatureAlgorithm<T>) method.invoke(null, supplier);
		}
		catch (NoSuchMethodException | SecurityException | InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
			throw new NoSuchAlgorithmException("unknown signature algorithm named " + name, e);
		}
	}

	/**
	 * Yields the signature algorithm for the given type of keys.
	 * 
	 * @param <T> the type of the values that get signed
	 * @param name the name of the algorithm, case-insensitive
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the given algorithm
	 */
	static <T> SignatureAlgorithm<T> mk(TYPES type, BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		return mk(type.name(), supplier);
	}

	/**
	 * The alternatives of signature algorithms currently implemented.
	 */
	public static enum TYPES {
		ED25519,
		EMPTY,
		QTESLA,
		SHA256DSA
	}
}