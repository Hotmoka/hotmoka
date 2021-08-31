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

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.stream.Collectors;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.crypto.BIP39Dictionary;
import io.hotmoka.crypto.SignatureAlgorithm;

/**
 * Shared code of signature algorithms.
 */
abstract class AbstractSignatureAlgorithm<T> implements SignatureAlgorithm<T> {

	private void writePemFile(byte[] key, String description, String filename) throws IOException {
		PemObject pemObject = new PemObject(description, key);

		try(PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(filename)))) {
			pemWriter.writeObject(pemObject);
		}
	}

	private void writePemFile(PrivateKey key, String filename) throws IOException {
		writePemFile(encodingOf(key), "PRIVATE KEY", filename + ".pri");
	}

	private void writePemFile(PublicKey key, String filename) throws IOException {
		writePemFile(encodingOf(key), "PUBLIC KEY", filename + ".pub");
	}

	private byte[] getPemFile(String file) throws IOException {
		try (PemReader reader = new PemReader(new FileReader(file))) {
			return reader.readPemObject().getContent();
		}
	}

	private byte[] getPrivatePemFile(String file) throws IOException {
		return getPemFile(file + ".pri");
	}

	private byte[] getPublicPemFile(String file) throws IOException {
		return getPemFile(file + ".pub");
	}

	
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
		SecureRandom random = new SecureRandom() {
			private final static long serialVersionUID = 1L;
			private final byte[] seed = mergeEntropyWithPassword();

			@Override
			public void nextBytes(byte[] bytes) {
				// copy the seed into the requested bytes
				System.arraycopy(seed, 0, bytes, 0, bytes.length);
			}

			private byte[] mergeEntropyWithPassword() {
				var words = new BIP39WordsImpl(entropy, dictionary);
		    	String mnemonic = words.stream().collect(Collectors.joining(" "));
		    	String salt = String.format("mnemonic%s", password);
		    	
		    	// 2048 iterations of the key-stretching algorithm PBKDF2 using HMAC-SHA512
		    	PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA512Digest());
		    	gen.init(mnemonic.getBytes(StandardCharsets.UTF_8), salt.getBytes(StandardCharsets.UTF_8), 2048);

		    	return ((KeyParameter) gen.generateDerivedParameters(512)).getKey();
		    }
		};

		try {
			return mkKeyPairGenerator(random).generateKeyPair();
		}
		catch (NoSuchProviderException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
    		throw InternalFailureException.of("unexpected exception", e);
    	}
    }

	@Override
	public KeyPair getKeyPair(byte[] entropy, String password) {
		return getKeyPair(entropy, BIP39Dictionary.ENGLISH_DICTIONARY, password);
	}

	@Override
	public byte[] encodingOf(PublicKey publicKey) {
		return publicKey.getEncoded();
	}

	@Override
	public byte[] encodingOf(PrivateKey privateKey) {
		return privateKey.getEncoded();
	}

	@Override
	public final void dumpAsPem(String filePrefix, KeyPair keys) throws IOException {
		writePemFile(keys.getPrivate(), filePrefix);
		writePemFile(keys.getPublic(), filePrefix);
	}

	@Override
	public final KeyPair readKeys(String filePrefix) throws IOException, InvalidKeySpecException {
		byte[] encodedPublicKey = getPublicPemFile(filePrefix);
		byte[] encodedPrivateKey = getPrivatePemFile(filePrefix);

		return new KeyPair(publicKeyFromEncoding(encodedPublicKey), privateKeyFromEncoding(encodedPrivateKey));
	}
}