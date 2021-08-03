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
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
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

	protected final void writePemFile(byte[] key, String description, String filename) throws IOException {
		PemObject pemObject = new PemObject(description, key);

		try(PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(filename)))) {
			pemWriter.writeObject(pemObject);
		}
	}

	protected final void writePemFile(Key key, String description, String filename) throws IOException {
		writePemFile(key.getEncoded(), description, filename);
	}

	protected byte[] getPemFile(String file) throws IOException {
		try (PemReader reader = new PemReader(new FileReader(file))) {
			return reader.readPemObject().getContent();
		}
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
		    	try {
					gen.init(mnemonic.getBytes("UTF_8"), salt.getBytes("UTF_8"), 2048);
				}
		    	catch (UnsupportedEncodingException e) {
		    		throw InternalFailureException.of("unexpected exception", e);
				}

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
}