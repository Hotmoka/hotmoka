/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.crypto.api;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.function.Function;

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
	 * Yields a public key that can be used with this signature, from
	 * its encoded version as a byte array.
	 * 
	 * @param encoding the encoded version of the public key
	 * @return the public key
	 * @throws InvalidKeySpecException if {@code encoding} does not match the expected specification
	 */
	PublicKey publicKeyFromEncoding(byte[] encoding) throws InvalidKeySpecException;

	/**
	 * Yields the encoded bytes of the given public key.
	 * 
	 * @param publicKey the public key
	 * @return the encoded bytes of {@code publicKey}
	 * @throws InvalidKeyException if the public key cannot be encoded
	 */
	byte[] encodingOf(PublicKey publicKey) throws InvalidKeyException;

	/**
	 * Yields a private key that can be used with this signature, from
	 * its encoded version as a byte array.
	 * 
	 * @param encoding the encoded version of the private key
	 * @return the private key
	 * @throws InvalidKeySpecException if {@code encoding} does not match the expected specification
	 */
	PrivateKey privateKeyFromEncoding(byte[] encoding) throws InvalidKeySpecException;

	/**
	 * Yields the encoded bytes of the given private key.
	 * 
	 * @param privateKey the private key
	 * @return the encoded bytes of {@code privateKey}
	 * @throws InvalidKeyException if the private key cannot be encoded
	 */
	byte[] encodingOf(PrivateKey privateKey) throws InvalidKeyException;

	/**
	 * Yields the type of this signature algotihm.
	 * 
	 * @return the type
	 */
	/**
	 * Yields the name of the algorithm.
	 * 
	 * @return the name of the algorithm
	 */
	String getName();

	/**
	 * Yields the supplier of this signature algorithm.
	 * 
	 * @return the supplier
	 */
	Supplier<T> getSupplier();

	/**
     * Creates a key pair from the given entropy and password.
     * 
     * @param entropy the entropy
     * @param dictionary the BIP39 dictionary to use for the encoding of the entropy
     * @param password data that gets hashed with the entropy to get the private key data
     * @return the key pair derived from entropy and password
     */
    KeyPair getKeyPair(byte[] entropy, BIP39Dictionary dictionary, String password);

    /**
     * Creates a key pair from the given entropy and password, by using the English dictionary.
     * 
     * @param entropy the entropy
     * @param password data that gets hashed with the entropy to get the private key data
     * @return the key pair derived from entropy and password
     */
    KeyPair getKeyPair(byte[] entropy, String password);

    /**
     * A supplier of a signature algorithm.
     * 
     * @param <T> the type of the objects that will get signed
     */
    interface Supplier<T> {

    	/**
    	 * Supplies a signature algorithm with this supplier.
    	 * 
    	 * @param toBytes a function applied to the objects to sign, to transform them into bytes that
    	 *                will then be signed
    	 * @return the signature algorithm
    	 * @throws NoSuchAlgorithmException if the signature algorithm is not available
    	 */
    	SignatureAlgorithm<T> get(Function<? super T, byte[]> toBytes) throws NoSuchAlgorithmException;
    }
}