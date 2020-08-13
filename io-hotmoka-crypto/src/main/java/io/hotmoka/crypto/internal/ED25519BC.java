/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hotmoka.crypto.internal;

import io.hotmoka.crypto.BytesSupplier;
import io.hotmoka.crypto.SignatureAlgorithm;
import java.io.IOException;
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
import java.util.Arrays;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.prng.RandomGenerator;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import static org.bouncycastle.jce.provider.BouncyCastleProvider.getPrivateKey;
import org.bouncycastle.util.Strings;

/**
 *
 * @author giovanni.antino@h2tcoin.com
 */
public class ED25519BC<T> implements SignatureAlgorithm<T> {

    /**
     * The actual signing algorithm.
     */
    private final Signature signature;

    /**
     * The key pair generator.
     */
    private final Ed25519KeyPairGenerator keyPairGen;

    /**
     * How values get transformed into bytes, before being hashed.
     */
    private final BytesSupplier<? super T> supplier;

    public ED25519BC(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        signature = Signature.getInstance("Ed25519");
        keyPairGen = new Ed25519KeyPairGenerator();
        keyPairGen.init(
                new Ed25519KeyGenerationParameters(
                        new SecureRandom()
                )
        );
        this.supplier = supplier;
    }

    @Override
    public KeyPair getKeyPair() {
        KeyPair keyPair = null;

        try {
            AsymmetricCipherKeyPair generatedKeyPair = keyPairGen.generateKeyPair();
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(generatedKeyPair.getPrivate());
            SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(generatedKeyPair.getPublic());
            PrivateKey privateKey = getPrivateKey(privateKeyInfo);
            PublicKey publicKey = BouncyCastleProvider.getPublicKey(publicKeyInfo);
            keyPair = new KeyPair(publicKey, privateKey);
            //return new KeyPair(generatedKeyPair.getPublic(), generatedKeyPair.getPrivate());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return keyPair;
    }

    @Override
    public byte[] sign(T what, PrivateKey privateKey) throws InvalidKeyException, SignatureException {
        byte[] bytes;

        try {
            bytes = supplier.get(what);
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        return keyFactory.generatePublic(pubKeySpec);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        ED25519BC<Object> eD25519BC = new ED25519BC<>(new DumbByteSuplier());
        KeyPair keyPair = eD25519BC.getKeyPair();
        System.out.println("Public: " + keyPair.getPublic());
        System.out.println("Public: " + Arrays.toString(keyPair.getPublic().getEncoded()));
        System.out.println("Private: " + keyPair.getPrivate());
        System.out.println("Private: " + Arrays.toString(keyPair.getPrivate().getEncoded()));
        String gatto = "gatto";

        try {
            byte[] sign = eD25519BC.sign(gatto, keyPair.getPrivate());
            System.out.println("Signature: " + Arrays.toString(sign));
            boolean verify = eD25519BC.verify(gatto, keyPair.getPublic(), sign);
            System.out.println("is valid signature: " + verify);
        } catch (InvalidKeyException | SignatureException ex) {
            ex.printStackTrace();
        }
        try {
            PublicKey publicKeyFromEncoded = eD25519BC.publicKeyFromEncoded(keyPair.getPublic().getEncoded());
            System.out.println("get public from encoded: " + publicKeyFromEncoded);
        } catch (NoSuchProviderException | InvalidKeySpecException ex) {
            ex.printStackTrace();
        }
    }

}
