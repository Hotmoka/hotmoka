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

package io.hotmoka.crypto.internal;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.stream.Collectors;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import io.hotmoka.crypto.api.BIP39Dictionary;

/**
 * A signature algorithm that hashes data with SHA256 and then
 * sign them with the DSA algorithm.
 */
public class SHA256DSA extends AbstractSignatureAlgorithmImpl {

	/**
	 * The actual signing algorithm.
	 */
	private final Signature signature;

	/**
	 * The key pair generator.
	 */
	private final KeyPairGenerator keyPairGenerator;

	/**
	 * The key factory.
	 */
	private final KeyFactory keyFactory;

	public SHA256DSA() throws NoSuchAlgorithmException {
		this.signature = Signature.getInstance("SHA256withDSA");
		this.keyPairGenerator = mkKeyPairGenerator(CryptoServicesRegistrar.getSecureRandom());

		try {
			this.keyFactory = KeyFactory.getInstance("DSA", "SUN");
		}
    	catch (NoSuchProviderException e) {
    		throw new NoSuchAlgorithmException(e);
    	}
	}

	@Override
	protected KeyPairGenerator mkKeyPairGenerator(SecureRandom random) throws NoSuchAlgorithmException {
		var keyPairGenerator = KeyPairGenerator.getInstance("DSA");
		keyPairGenerator.initialize(2048, random);
		return keyPairGenerator;
	}

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

				return ((KeyParameter) gen.generateDerivedParameters(1792)).getKey();
			}
		};

		try {
			return mkKeyPairGenerator(random).generateKeyPair();
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unexpected exception", e);
		}
	}

	@Override
	public KeyPair getKeyPair() {
		return keyPairGenerator.generateKeyPair();
	}

	@Override
	protected byte[] sign(byte[] bytes, PrivateKey privateKey) throws InvalidKeyException, SignatureException {
		synchronized (signature) {
			signature.initSign(privateKey);
			signature.update(bytes);
			return signature.sign();
		}
	}

	@Override
	protected boolean verify(byte[] bytes, PublicKey publicKey, byte[] signature) throws InvalidKeyException, SignatureException {
		synchronized (this.signature) { 
			this.signature.initVerify(publicKey);
			this.signature.update(bytes);
			return this.signature.verify(signature);
		}
	}

	@Override
	public PublicKey publicKeyFromEncoding(byte[] encoded) throws InvalidKeySpecException {
		var pubKeySpec = new X509EncodedKeySpec(encoded);
		return keyFactory.generatePublic(pubKeySpec);
	}

	@Override
	public PrivateKey privateKeyFromEncoding(byte[] encoded) throws InvalidKeySpecException {
		return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
	}
}