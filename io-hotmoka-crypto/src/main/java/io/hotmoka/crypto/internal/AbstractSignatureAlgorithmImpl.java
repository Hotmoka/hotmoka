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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hotmoka.crypto.internal;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import io.hotmoka.crypto.api.BIP39Dictionary;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.crypto.api.Verifier;

/**
 * Partial implementation of a signature algorithm.
 */
public abstract class AbstractSignatureAlgorithmImpl implements SignatureAlgorithm {

	/**
	 * Yields the signature of the given value, by using the given private key.
	 * 
	 * @param <T> the type of values that get signed
	 * @param what the value to sign
	 * @param toBytes a function applied to transform the value into bytes before signing
	 * @param privateKey the private key used for signing
	 * @return the signature
	 * @throws InvalidKeyException if the provided private key is invalid
	 * @throws SignatureException if the value cannot be signed
	 */
	private <T> byte[] sign(T what, Function<? super T, byte[]> toBytes, PrivateKey privateKey) throws InvalidKeyException, SignatureException {
		try {
			return sign(toBytes.apply(what), privateKey);
        }
        catch (Exception e) {
            throw new SignatureException("Cannot transform the value into bytes before signing", e);
        }
	}

	/**
	 * Verifies that the given signature is derived from the given value, by using the given public key.
	 * 
	 * @param <T> the type of values that get verified
	 * @param what the value whose signature gets verified
	 * @param toBytes a function applied to transform the value into bytes before verification
	 * @param publicKey the public key; its corresponding private key should have been used for signing
	 * @param signature the signature to verify
	 * @return true if and only if the signature matches
	 * @throws InvalidKeyException if the provided public key is invalid
	 * @throws SignatureException if the value cannot be signed
	 */
	private <T> boolean verify(T what, Function<? super T, byte[]> toBytes, PublicKey publicKey, byte[] signature) throws InvalidKeyException, SignatureException {
		try {
			return verify(toBytes.apply(what), publicKey, signature);
        }
        catch (Exception e) {
            throw new SignatureException("Cannot transform the value into bytes before signature verification", e);
        }
	}

	/**
	 * Verifies that the given signature is derived from the given bytes, by using the given public key.
	 * 
	 * @param bytes the bytes
	 * @param publicKey the public key; its corresponding private key should have been used for signing
	 * @param signature the signature to verify
	 * @return true if and only if the signature matches
	 * @throws InvalidKeyException if the provided public key is invalid
	 * @throws SignatureException if the value cannot be signed
	 */
	protected abstract boolean verify(byte[] bytes, PublicKey publicKey, byte[] signature) throws InvalidKeyException, SignatureException;

	/**
	 * Yields the signature of the given bytes, by using the given private key.
	 * 
	 * @param bytes the bytes to sign
	 * @param privateKey the private key used for signing
	 * @return the signature
	 * @throws InvalidKeyException if the provided private key is invalid
	 * @throws SignatureException if the value cannot be signed
	 */
	protected abstract byte[] sign(byte[] bytes, PrivateKey privateKey) throws InvalidKeyException, SignatureException;

	/**
	 * Creates a key pair generator for this signature algorithm.
	 * 
	 * @param random the generator of entropy to use for the key pair generator
	 * @return the key pair generator
	 */
	protected abstract KeyPairGenerator mkKeyPairGenerator(SecureRandom random) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException;

	@Override
	public KeyPair getKeyPair(byte[] entropy, BIP39Dictionary dictionary, String password) {
		// we create a random object that we use only once and always provides the seed
		var random = new SecureRandom() {
			private final static long serialVersionUID = 1L;
			private final byte[] seed = mergeEntropyWithPassword();

			@Override
			public void nextBytes(byte[] bytes) {
				// copy the seed into the requested bytes
				System.arraycopy(seed, 0, bytes, 0, bytes.length);
			}

			private byte[] mergeEntropyWithPassword() {
				var words = new BIP39MnemonicImpl(entropy, dictionary);
		    	String mnemonic = words.stream().collect(Collectors.joining(" "));
		    	String salt = String.format("mnemonic%s", password);
		    	
		    	// 2048 iterations of the key-stretching algorithm PBKDF2 using HMAC-SHA512
		    	var gen = new PKCS5S2ParametersGenerator(new SHA512Digest());
		    	gen.init(mnemonic.getBytes(StandardCharsets.UTF_8), salt.getBytes(StandardCharsets.UTF_8), 2048);

		    	return ((KeyParameter) gen.generateDerivedParameters(512)).getKey();
		    }
		};

		try {
			return mkKeyPairGenerator(random).generateKeyPair();
		}
		catch (NoSuchProviderException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
    		throw new RuntimeException("unexpected exception", e);
    	}
    }

	@Override
	public final <T> Signer<T> getSigner(PrivateKey key, Function<? super T, byte[]> toBytes) {
		return what -> sign(what, toBytes, key);
	}

	@Override
	public final <T> Verifier<T> getVerifier(PublicKey key, Function<? super T, byte[]> toBytes) {
		return (what, bytes) -> verify(what, toBytes, key, bytes);
	}

	@Override
	public KeyPair getKeyPair(byte[] entropy, String password) {
		return getKeyPair(entropy, io.hotmoka.crypto.BIP39Dictionaries.ENGLISH_DICTIONARY, password);
	}

	@Override
	public byte[] encodingOf(PublicKey publicKey) throws InvalidKeyException {
		return publicKey.getEncoded();
	}

	@Override
	public byte[] encodingOf(PrivateKey privateKey) throws InvalidKeyException {
		return privateKey.getEncoded();
	}

	@Override
	public boolean equals(Object other) {
		return other != null && getClass() == other.getClass();
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public String getName() {
		return getClass().getSimpleName().toLowerCase();
	}

	@Override
	public String toString() {
		return getName();
	}
}