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
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.pqc.crypto.qtesla.QTESLASigner;
import org.bouncycastle.pqc.crypto.util.PrivateKeyFactory;
import org.bouncycastle.pqc.crypto.util.PublicKeyFactory;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.QTESLAParameterSpec;

/**
 * A signature algorithm that signs data with the qTESLA-p-I signature scheme.
 */
public class QTESLA1 extends AbstractSignatureAlgorithmImpl {

    /**
     * The key pair generator.
     */
    private final KeyPairGenerator keyPairGenerator;

    /**
     * The key factory.
     */
    private final KeyFactory keyFactory;

    /**
     * The actual signing algorithm.
     */
    private final QTESLASigner signer;

    public QTESLA1() throws NoSuchAlgorithmException {
    	try {
    		ensureProvider();
    		this.keyPairGenerator = mkKeyPairGenerator(CryptoServicesRegistrar.getSecureRandom());
    		this.signer = new QTESLASigner();
    		this.keyFactory = KeyFactory.getInstance("qTESLA", "BCPQC");
    	}
    	catch (NoSuchAlgorithmException e) {
    		throw e;
    	}
    	catch (NoSuchProviderException | InvalidAlgorithmParameterException e) {
    		throw new NoSuchAlgorithmException(e);
    	}
    }

    @Override
	protected KeyPairGenerator mkKeyPairGenerator(SecureRandom random) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		var keyPairGenerator = KeyPairGenerator.getInstance("qTESLA", "BCPQC");
		keyPairGenerator.initialize(new QTESLAParameterSpec(QTESLAParameterSpec.PROVABLY_SECURE_I), random);
		return keyPairGenerator;
	}

	@Override
    public KeyPair getKeyPair() {
    	return keyPairGenerator.generateKeyPair();
    }

    @Override
    protected byte[] sign(byte[] bytes, PrivateKey privateKey) throws SignatureException {
        synchronized (signer) {
            try {
                PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(new PKCS8EncodedKeySpec(encodingOf(privateKey)).getEncoded()));
                signer.init(true, PrivateKeyFactory.createKey(privateKeyInfo));
                return signer.generateSignature(bytes);
            }
            catch (Exception e) {
                throw new SignatureException("cannot generate signature", e);
            }
        }
    }

    @Override
    protected boolean verify(byte[] bytes, PublicKey publicKey, byte[] signature) throws InvalidKeyException, SignatureException {
        synchronized (signer) {
            try {
                SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(new X509EncodedKeySpec(encodingOf(publicKey)).getEncoded());
                signer.init(false, PublicKeyFactory.createKey(subjectPublicKeyInfo));
                return signer.verifySignature(bytes, signature);
            }
            catch (InvalidKeyException e) {
            	throw e;
            }
            catch (Exception e){
                throw new SignatureException("cannot verify signature", e);
            }
        }
    }

    @Override
    public PublicKey publicKeyFromEncoding(byte[] encoded) throws InvalidKeySpecException {
        return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
    }

    @Override
	public PrivateKey privateKeyFromEncoding(byte[] encoded) throws InvalidKeySpecException {
		return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
	}

    private static void ensureProvider() {
    	 if (Security.getProvider(BouncyCastlePQCProvider.PROVIDER_NAME) == null)
             Security.addProvider(new BouncyCastlePQCProvider());
	}
}