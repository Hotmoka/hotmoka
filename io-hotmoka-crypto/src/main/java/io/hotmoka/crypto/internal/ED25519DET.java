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
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.spec.EdDSAParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * A signature algorithm that uses the ED25519 cryptography. It generates
 * keys in a deterministic order, hence must NOT be used in production.
 * It is useful instead for testing, since it makes deterministic the
 * sequence of keys of the accounts in the tests and consequently
 * also the gas costs of such accounts when they are put into maps, for instance.
 */
public class ED25519DET extends AbstractSignatureAlgorithmImpl {

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

    public ED25519DET() throws NoSuchAlgorithmException {
    	try {
    		ensureProvider();
    		this.signature = Signature.getInstance("Ed25519");
    		this.keyFactory = KeyFactory.getInstance("Ed25519", "BC");
    		var random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed("nel mezzo del cammin di nostra vita".getBytes(StandardCharsets.US_ASCII));
    		this.keyPairGenerator = mkKeyPairGenerator(random);
    	}
    	catch (NoSuchProviderException | InvalidAlgorithmParameterException e) {
    		throw new NoSuchAlgorithmException(e);
    	}
    }

    @Override
	protected KeyPairGenerator mkKeyPairGenerator(SecureRandom random) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
    	var keyPairGenerator = KeyPairGenerator.getInstance("Ed25519", "BC");
		keyPairGenerator.initialize(new EdDSAParameterSpec(EdDSAParameterSpec.Ed25519), random);
		return keyPairGenerator;
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
    	try {
    		Ed25519PublicKeyParameters publicKeyParams = new Ed25519PublicKeyParameters(encoded, 0);
			return keyFactory.generatePublic(new X509EncodedKeySpec(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKeyParams).getEncoded()));
		}
		catch (IOException | ArrayIndexOutOfBoundsException e) {
			throw new InvalidKeySpecException(e);
		}
    }

    @Override
   	public PrivateKey privateKeyFromEncoding(byte[] encoded) throws InvalidKeySpecException {
    	try {
    		var privateKeyParams = new Ed25519PrivateKeyParameters(encoded, 0);
			return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(PrivateKeyInfoFactory.createPrivateKeyInfo(privateKeyParams).getEncoded()));
		}
		catch (IOException | ArrayIndexOutOfBoundsException  e) {
			throw new InvalidKeySpecException(e);
		}
   	}

    @Override
    public byte[] encodingOf(PublicKey publicKey) {
    	// we drop the initial 12 bytes
		Ed25519PublicKeyParameters publicKeyParams = new Ed25519PublicKeyParameters(publicKey.getEncoded(), 12);
		return publicKeyParams.getEncoded();
    }

    @Override
    public byte[] encodingOf(PrivateKey privateKey) throws InvalidKeyException {
    	try {
    		PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(new PKCS8EncodedKeySpec(privateKey.getEncoded()).getEncoded()));
    		ASN1Encodable privateKey2 = privateKeyInfo.parsePrivateKey();
    		return new Ed25519PrivateKeyParameters(((ASN1OctetString) privateKey2).getOctets(), 0).getEncoded();
    	}
    	catch (IOException e) {
    		throw new InvalidKeyException("cannot encode the private key", e);
    	}
    }

    private static void ensureProvider() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
	        Security.addProvider(new BouncyCastleProvider());
	}
}