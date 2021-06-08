package io.hotmoka.tests;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Base64;

public class SignedRequests {
    private static KeyPair keyPair;

    static {
        keyPair = loadKeys();
    }


    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) NonVoidMethod = M93FSC2jthDeWkcEDNFMnl7G88i9MXeTJfH0R2nzaHfodQNwqm9udQ2aPpZfKMNSQ1y2EvS1w0Eo1GcEzD2rDQ==")
    public void testInstanceMethodCallNonVoid() throws Exception {

        InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.mk("ed25519"), keyPair),
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                CodeSignature.GET_GAMETE,
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO)
        );

        String signature = toBase64(request.getSignature());
        Assertions.assertEquals("M93FSC2jthDeWkcEDNFMnl7G88i9MXeTJfH0R2nzaHfodQNwqm9udQ2aPpZfKMNSQ1y2EvS1w0Eo1GcEzD2rDQ==", signature);
    }


    protected static KeyPair loadKeys() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Paths.get("ed25519SignatureTests.keys").toFile()))) {
            return (KeyPair) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static String toBase64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes));
    }
}
