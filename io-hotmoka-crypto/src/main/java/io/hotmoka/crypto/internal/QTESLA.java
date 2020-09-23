package io.hotmoka.crypto.internal;


import io.hotmoka.crypto.BytesSupplier;
import io.hotmoka.crypto.SignatureAlgorithm;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.pqc.crypto.qtesla.*;
import org.bouncycastle.pqc.crypto.util.PrivateKeyFactory;
import org.bouncycastle.pqc.crypto.util.PublicKeyFactory;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.provider.qtesla.BCqTESLAPrivateKey;
import org.bouncycastle.pqc.jcajce.provider.qtesla.BCqTESLAPublicKey;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * A signature algorithm that signs data with the qTESLA-p-III signature scheme.
 *
 * @param <T> the type of values that gets signed
 */
public class QTESLA<T> implements SignatureAlgorithm<T> {

    /**
     * How values get transformed into bytes, before being hashed.
     */
    private final BytesSupplier<? super T> supplier;

    /**
     * The key pair generator.
     */
    private final QTESLAKeyPairGenerator keyPairGenerator;

    /**
     * The actual signing algorithm.
     */
    private final QTESLASigner signer;


    public QTESLA(BytesSupplier<? super T> supplier) {
        this.supplier = supplier;
        this.keyPairGenerator = new QTESLAKeyPairGenerator();
        keyPairGenerator.init(new QTESLAKeyGenerationParameters(QTESLASecurityCategory.PROVABLY_SECURE_III, CryptoServicesRegistrar.getSecureRandom()));
        this.signer = new QTESLASigner();
    }

    @Override
    public KeyPair getKeyPair() {
    	AsymmetricCipherKeyPair keyPairGen = keyPairGenerator.generateKeyPair();
        QTESLAPublicKeyParameters pub = (QTESLAPublicKeyParameters) keyPairGen.getPublic();
        QTESLAPrivateKeyParameters priv = (QTESLAPrivateKeyParameters) keyPairGen.getPrivate();
        return new KeyPair(new BCqTESLAPublicKey(pub), new BCqTESLAPrivateKey(priv));
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

        synchronized (signer) {
            try {
                PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(new PKCS8EncodedKeySpec(privateKey.getEncoded()).getEncoded()));
                signer.init(true, PrivateKeyFactory.createKey(privateKeyInfo));
                return signer.generateSignature(bytes);
            }
            catch (Exception e) {
            	e.printStackTrace();
                throw new SignatureException("cannot generate signature", e);
            }
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
    public PublicKey publicKeyFromEncoded(byte[] encoded) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {

        if (Security.getProvider(BouncyCastlePQCProvider.PROVIDER_NAME) == null)
            Security.addProvider(new BouncyCastlePQCProvider());

        try {
            return new BCqTESLAPublicKey(SubjectPublicKeyInfo.getInstance(new X509EncodedKeySpec(encoded).getEncoded()));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
