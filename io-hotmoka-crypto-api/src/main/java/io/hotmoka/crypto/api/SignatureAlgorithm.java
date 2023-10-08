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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.function.Function;

/**
 * An algorithm for signing values and verifying such signatures back.
 */
public interface SignatureAlgorithm {

	/**
	 * Yields a pair of keys (private/public) that can be used with
	 * this signature algorithm.
	 * 
	 * @return the pair of keys
	 */
	KeyPair getKeyPair();

	/**
	 * Yields a signer with this signature algorithm.
	 * 
	 * @param <T> the type of values that get signed
	 * @param key the private key that will be used for signing
	 * @param toBytes the function to use to transform the value into bytes before signing
	 * @return the signer
	 */
	<T> Signer<T> getSigner(PrivateKey key, Function<? super T, byte[]> toBytes);

	/**
	 * Yields a verifier with this signature algorithm.
	 * 
	 * @param <T> the type of values that get verified
	 * @param key the public key that will be used for verification
	 * @param toBytes the function to use to transform the value into bytes before verification
	 * @return the verifier
	 */
	<T> Verifier<T> getVerifier(PublicKey key, Function<? super T, byte[]> toBytes);

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
	 * Yields the name of the algorithm.
	 * 
	 * @return the name of the algorithm
	 */
	String getName();

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
     * Determines if this signature algorithm is the same as another.
     * 
     * @param other the other object
     * @return true only if other is the same signature algorithm
     */
    @Override
    boolean equals(Object other);

    @Override
    int hashCode();

    @Override
    String toString();
}