package io.hotmoka.crypto.internal;


import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
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

import io.hotmoka.crypto.BytesSupplier;
import io.hotmoka.crypto.SignatureAlgorithm;

/**
 * A signature algorithm that signs data with the qTESLA-p-III signature scheme.
 *
 * @param <T> the type of values that gets signed
 */
public class QTESLA3<T> implements SignatureAlgorithm<T> {

    /**
     * How values get transformed into bytes, before being hashed.
     */
    private final BytesSupplier<? super T> supplier;

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

    public QTESLA3(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
    	try {
    		ensureProvider();
    		this.supplier = supplier;
    		this.keyPairGenerator = KeyPairGenerator.getInstance("qTESLA", "BCPQC");
    		keyPairGenerator.initialize(new QTESLAParameterSpec(QTESLAParameterSpec.PROVABLY_SECURE_III), CryptoServicesRegistrar.getSecureRandom());
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
    public KeyPair getKeyPair() {
    	return keyPairGenerator.generateKeyPair();
    }

    @Override
    public byte[] sign(T what, PrivateKey privateKey) throws SignatureException {
        byte[] bytes;

        try {
            bytes = supplier.get(what);
        }
        catch (Exception e) {
            throw new SignatureException("cannot transform value into bytes before signing", e);
        }

        synchronized (signer) {
            try {
                PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(new PKCS8EncodedKeySpec(privateKey.getEncoded()).getEncoded()));
                signer.init(true, PrivateKeyFactory.createKey(privateKeyInfo));
                return signer.generateSignature(bytes);
            }
            catch (Exception e) {
                throw new SignatureException("cannot generate signature", e);
            }
        }
    }

    @Override
    public boolean verify(T what, PublicKey publicKey, byte[] signature) throws SignatureException {
        byte[] bytes;

        try {
            bytes = supplier.get(what);
        }
        catch (Exception e) {
            throw new SignatureException("cannot transform value into bytes before verifying the signature", e);
        }

        synchronized (signer) {
            try {
                SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(new X509EncodedKeySpec(publicKey.getEncoded()).getEncoded());
                signer.init(false, PublicKeyFactory.createKey(subjectPublicKeyInfo));
                return signer.verifySignature(bytes, signature);
            }
            catch (Exception e){
                throw new SignatureException("cannot verify signature", e);
            }
        }
    }

    @Override
    public PublicKey publicKeyFromEncoded(byte[] encoded) throws InvalidKeySpecException {
        return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static void ensureProvider() {
    	 if (Security.getProvider(BouncyCastlePQCProvider.PROVIDER_NAME) == null)
             Security.addProvider(new BouncyCastlePQCProvider());
	}

	@Override
	public String getName() {
		return "qtesla3";
	}
}