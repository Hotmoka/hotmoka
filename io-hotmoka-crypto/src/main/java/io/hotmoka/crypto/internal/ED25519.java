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

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.spec.EdDSAParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.hotmoka.crypto.BytesSupplier;

/**
 * A signature algorithm that uses the ED25519 cryptography.
 */
public class ED25519<T> extends AbstractSignatureAlgorithm<T> {

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

    /**
     * How values get transformed into bytes, before being hashed.
     */
    private final BytesSupplier<? super T> supplier;

    public ED25519(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
    	try {
    		ensureProvider();
    		this.signature = Signature.getInstance("Ed25519");
    		this.keyFactory = KeyFactory.getInstance("Ed25519", "BC");
    		this.keyPairGenerator = KeyPairGenerator.getInstance("Ed25519", "BC");
    		keyPairGenerator.initialize(new EdDSAParameterSpec(EdDSAParameterSpec.Ed25519), CryptoServicesRegistrar.getSecureRandom());
    		this.supplier = supplier;
    	}
    	catch (NoSuchProviderException | InvalidAlgorithmParameterException e) {
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
    public PublicKey publicKeyFromEncoded(byte[] encoded) throws InvalidKeySpecException {
        return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static void ensureProvider() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
	        Security.addProvider(new BouncyCastleProvider());
	}

	@Override
	public String getName() {
		return "ed25519";
	}

	@Override
	public void dumpAsPem(String filePrefix, KeyPair keys) throws IOException {
		ensureProvider();

		// private key
		PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(new PKCS8EncodedKeySpec(keys.getPrivate().getEncoded()).getEncoded()));
		ASN1Encodable privateKey = privateKeyInfo.parsePrivateKey();
		Ed25519PrivateKeyParameters privateKeyParams = new Ed25519PrivateKeyParameters(((ASN1OctetString) privateKey).getOctets(), 0);
		writePemFile(privateKeyParams.getEncoded(), "PRIVATE KEY", filePrefix + ".pri");

		// public key
		ASN1BitString publicKeyData = privateKeyInfo.getPublicKeyData();
		Ed25519PublicKeyParameters publicKeyParams = new Ed25519PublicKeyParameters(publicKeyData.getOctets(), 0);
		writePemFile(publicKeyParams.getEncoded(), "PUBLIC KEY", filePrefix + ".pub");
	}

	@Override
	public KeyPair readKeys(String filePrefix) throws IOException, InvalidKeySpecException {
		byte[] encodedPublicKey = getPemFile(filePrefix + ".pub");
		byte[] encodedPrivateKey = getPemFile(filePrefix + ".pri");

		// private key
		Ed25519PrivateKeyParameters privateKeyParams = new Ed25519PrivateKeyParameters(encodedPrivateKey, 0);
		byte[] pkcs8Encoded = PrivateKeyInfoFactory.createPrivateKeyInfo(privateKeyParams).getEncoded();

		// public key
		Ed25519PublicKeyParameters publicKeyParams = new Ed25519PublicKeyParameters(encodedPublicKey, 0);
		byte[] spkiEncoded = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKeyParams).getEncoded();

		// key factory
		PublicKey publicKeyObj = keyFactory.generatePublic(new X509EncodedKeySpec(spkiEncoded));
		PrivateKey privateKeyObj = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8Encoded));

		return new KeyPair(publicKeyObj, privateKeyObj);
	}
}