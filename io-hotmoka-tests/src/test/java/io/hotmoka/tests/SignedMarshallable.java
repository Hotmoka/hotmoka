package io.hotmoka.tests;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.util.Base64;

public class SignedMarshallable {
    private static KeyPair keyPair;

    static {
        Security.addProvider(new BouncyCastleProvider());
        keyPair = loadKeys();
    }


    @Test
    @DisplayName("Signing a string")
    public void testSignAString() throws Exception {
        String signature = signAndEncodeToBase64("hello".getBytes(), keyPair.getPrivate());
        Assertions.assertEquals("Zest4OcIbf6LLGkXPw7zOL4WTTSNUyRO/4ipi/UE6bVvdx8hRUl5nmjweF1/7TnIrrgtdhK8gWpu3XAz78H6Bw==", signature);
    }

    protected static KeyPair loadKeys() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Paths.get("ed25519SignatureTests.keys").toFile()))) {
            return (KeyPair) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static String signAndEncodeToBase64(byte[] what, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(privateKey);
        signature.update(what);
        return toBase64(signature.sign());
    }


    protected static byte[] sign(byte[] what, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(privateKey);
        signature.update(what);
        return signature.sign();
    }

    protected static String toBase64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes));
    }


}
