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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import io.hotmoka.crypto.BytesSupplier;

/**
 * A signature algorithm that hashes data with SHA256 and then
 * sign them with the DSA algorithm.
 * 
 * @param <T> the type of values that get signed
 */
public class SHA256DSA<T> extends AbstractSignatureAlgorithm<T> {

	/**
	 * The actual signing algorithm.
	 */
	private final Signature signature;

	/**
	 * The key pair generator.
	 */
	private final KeyPairGenerator keyPairGenerator;

	/**
	 * How values get transformed into bytes, before being hashed.
	 */
	private final BytesSupplier<? super T> supplier;

	/**
	 * The key factory.
	 */
	private final KeyFactory keyFactory;

	public SHA256DSA(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		this.signature = Signature.getInstance("SHA256withDSA");
		this.keyPairGenerator = KeyPairGenerator.getInstance("DSA");
		this.keyPairGenerator.initialize(2048);
		this.supplier = supplier;

		try {
			this.keyFactory = KeyFactory.getInstance("DSA", "SUN");
		}
    	catch (NoSuchProviderException e) {
    		throw new NoSuchAlgorithmException(e);
    	}
	}

	@Override
	public KeyPair getKeyPair() {
		return keyPairGenerator.generateKeyPair();
	}

	@Override
	public byte[] sign(T what, PrivateKey privateKey) throws InvalidKeyException, SignatureException {
		byte[] bytes;

		try {
			bytes = supplier.get(what);
		}
		catch (Exception e) {
			throw new SignatureException("cannot transform value into bytes before signing", e);
		}

		synchronized (signature) {
			signature.initSign(privateKey);
			signature.update(bytes);
			return signature.sign();
		}
	}

	@Override
	public boolean verify(T what, PublicKey publicKey, byte[] signature) throws InvalidKeyException, SignatureException {
		byte[] bytes;

		try {
			bytes = supplier.get(what);
		}
		catch (Exception e) {
			throw new SignatureException("cannot transform value into bytes before verifying the signature", e);
		}

		synchronized (this.signature) { 
			this.signature.initVerify(publicKey);
			this.signature.update(bytes);
			return this.signature.verify(signature);
		}
	}

	@Override
	public PublicKey publicKeyFromEncoded(byte[] encoded) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encoded);
		return keyFactory.generatePublic(pubKeySpec);
	}

	@Override
	public String getName() {
		return "sha256dsa";
	}

	@Override
	public void dumpAsPem(String filePrefix, KeyPair keys) throws IOException {
		writePemFile(keys.getPrivate(), "PRIVATE KEY", filePrefix + ".pri");
		writePemFile(keys.getPublic(), "PUBLIC KEY", filePrefix + ".pub");
	}

	@Override
	public KeyPair readKeys(String filePrefix) throws IOException, InvalidKeySpecException {
		byte[] encodedPublicKey = getPemFile(filePrefix + ".pub");
		byte[] encodedPrivateKey = getPemFile(filePrefix + ".pri");

		PublicKey publicKeyObj = keyFactory.generatePublic(new X509EncodedKeySpec(encodedPublicKey));
		PrivateKey privateKeyObj = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedPrivateKey));

		return new KeyPair(publicKeyObj, privateKeyObj);
	}
}