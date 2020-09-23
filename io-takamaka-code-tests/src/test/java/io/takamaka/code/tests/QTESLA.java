package io.takamaka.code.tests;

import io.hotmoka.crypto.SignatureAlgorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.security.KeyPair;
import java.security.PublicKey;

public class QTESLA {
    private final static String data = "HELLO QTESLA SCHEME";

    @Test
    @DisplayName("sign data with qtesla signature")
    void sign() throws Exception {
        SignatureAlgorithm<String> qTesla = SignatureAlgorithm.qtesla(String::getBytes);

        KeyPair keyPair = qTesla.getKeyPair();
        byte[] signed = qTesla.sign(data, keyPair.getPrivate());

        boolean isDataVerifiedCorrectly = qTesla.verify(data, keyPair.getPublic(), signed);
        boolean isCorruptedData = !qTesla.verify(data + "corrupted", keyPair.getPublic(), signed);

        Assert.isTrue(isDataVerifiedCorrectly, "data is not verified correctly");
        Assert.isTrue(isCorruptedData, "corrupted data is verified");
    }

    @Test
    @DisplayName("create the public key from the encoded public key")
    void testEncodedPublicKey() throws Exception {
        SignatureAlgorithm<String> qTesla = SignatureAlgorithm.qtesla(String::getBytes);

        KeyPair keyPair = qTesla.getKeyPair();
        byte[] signed = qTesla.sign(data, keyPair.getPrivate());

        boolean isDataVerifiedCorrectly = qTesla.verify(data, keyPair.getPublic(), signed);
        boolean isCorruptedData = !qTesla.verify(data + "corrupted", keyPair.getPublic(), signed);

        PublicKey publicKey = qTesla.publicKeyFromEncoded(keyPair.getPublic().getEncoded());
        boolean isDataVerifiedCorrectlyWithEncodedKey = qTesla.verify(data, publicKey, signed);
        boolean isCorruptedDataWithEncodedKey = !qTesla.verify(data + "corrupted", publicKey, signed);

        Assert.isTrue(isDataVerifiedCorrectly, "data is not verified correctly");
        Assert.isTrue(isCorruptedData, "corrupted data is verified");
        Assert.isTrue(isDataVerifiedCorrectlyWithEncodedKey, "data is not verified correctly with the encoded key");
        Assert.isTrue(isCorruptedDataWithEncodedKey, "corrupted data is verified with the encoded key");

        Assert.isTrue(keyPair.getPublic().equals(publicKey), "the public keys do not match");
    }
}
