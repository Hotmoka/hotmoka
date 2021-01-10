package io.takamaka.code.tests;

import io.hotmoka.crypto.SignatureAlgorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.security.KeyPair;
import java.security.PublicKey;

public class QTESLA3 {
    private final static String data = "HELLO QTESLA SCHEME";

    @Test
    @DisplayName("sign data with qtesla signature")
    void sign() throws Exception {
        SignatureAlgorithm<String> qTesla3 = SignatureAlgorithm.qtesla3(String::getBytes);

        KeyPair keyPair = qTesla3.getKeyPair();
        byte[] signed = qTesla3.sign(data, keyPair.getPrivate());

        boolean isDataVerifiedCorrectly = qTesla3.verify(data, keyPair.getPublic(), signed);
        boolean isCorruptedData = !qTesla3.verify(data + "corrupted", keyPair.getPublic(), signed);

        Assert.isTrue(isDataVerifiedCorrectly, "data is not verified correctly");
        Assert.isTrue(isCorruptedData, "corrupted data is verified");
    }

    @Test
    @DisplayName("create the public key from the encoded public key")
    void testEncodedPublicKey() throws Exception {
        SignatureAlgorithm<String> qTesla3 = SignatureAlgorithm.qtesla3(String::getBytes);

        KeyPair keyPair = qTesla3.getKeyPair();
        byte[] signed = qTesla3.sign(data, keyPair.getPrivate());

        boolean isDataVerifiedCorrectly = qTesla3.verify(data, keyPair.getPublic(), signed);
        boolean isCorruptedData = !qTesla3.verify(data + "corrupted", keyPair.getPublic(), signed);

        PublicKey publicKey = qTesla3.publicKeyFromEncoded(keyPair.getPublic().getEncoded());
        boolean isDataVerifiedCorrectlyWithEncodedKey = qTesla3.verify(data, publicKey, signed);
        boolean isCorruptedDataWithEncodedKey = !qTesla3.verify(data + "corrupted", publicKey, signed);

        Assert.isTrue(isDataVerifiedCorrectly, "data is not verified correctly");
        Assert.isTrue(isCorruptedData, "corrupted data is verified");
        Assert.isTrue(isDataVerifiedCorrectlyWithEncodedKey, "data is not verified correctly with the encoded key");
        Assert.isTrue(isCorruptedDataWithEncodedKey, "corrupted data is verified with the encoded key");

        Assert.isTrue(keyPair.getPublic().equals(publicKey), "the public keys do not match");
    }
}