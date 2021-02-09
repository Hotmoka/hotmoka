/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hotmoka.crypto.internal;

import java.io.UnsupportedEncodingException;
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
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.jcajce.spec.EdDSAParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.hotmoka.crypto.BytesSupplier;
import io.hotmoka.crypto.SignatureAlgorithm;

/**
 * A signature algorithm that uses the ED25519 cryptography. It generates
 * keys in a deterministic order, hence must NOT be used in production.
 * It is useful instead for testing, since it makes deterministic the
 * sequence of keys of the accounts in the tests and consequently
 * also the gas costs of such accounts when they are put into maps, for instance.
 */
public class ED25519DET<T> implements SignatureAlgorithm<T> {

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

    public ED25519DET(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
    	try {
    		ensureProvider();
    		this.signature = Signature.getInstance("Ed25519");
    		this.keyFactory = KeyFactory.getInstance("Ed25519", "BC");
    		this.keyPairGenerator = KeyPairGenerator.getInstance("Ed25519", "BC");
    		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed("nel mezzo del cammin di nostra vita".getBytes("us-ascii"));
    		keyPairGenerator.initialize(new EdDSAParameterSpec(EdDSAParameterSpec.Ed25519), random);
    		this.supplier = supplier;
    	}
    	catch (NoSuchProviderException | InvalidAlgorithmParameterException | UnsupportedEncodingException e) {
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
        return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static void ensureProvider() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
	        Security.addProvider(new BouncyCastleProvider());
	}

	@Override
	public String getName() {
		return "ed25519det";
	}
}