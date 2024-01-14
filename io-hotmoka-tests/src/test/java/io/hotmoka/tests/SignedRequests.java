package io.hotmoka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.KeyPair;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.SignatureAlgorithms;

public class SignedRequests {
    private static final KeyPair keys;

    static {
        try {
            keys = SignatureAlgorithms.ed25519().getKeyPair(Hex.fromHexString("64ea6e847fd7c3c5403871f9e57d9f48"), "mysecret");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    @DisplayName("new ConstructorCallTransactionRequest(..) manifest")
    public void testConstructorCallTransactionRequest() throws Exception {
        var constructorSignature = new ConstructorSignature(
        		StorageTypes.MANIFEST,
                StorageTypes.BIG_INTEGER
        );

        var request = new ConstructorCallTransactionRequest(
        		SignatureAlgorithms.ed25519().getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
        		StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(11500),
                BigInteger.valueOf(500),
                TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                constructorSignature,
                StorageValues.bigIntegerOf(999)
        );

        String signature = toBase64(request.getSignature());
        assertEquals("8xMW6vu9SYIFmZkGq906s7rvsclXTTJmA2mpc7QTf9Tgz7WeuJMByAX5ihBKbq5m8dmR9PD/Uv7ALmSL0iJGDg==", signature);
    }

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) getGamete")
    public void testNonVoidInstanceMethodCallTransactionRequest() throws Exception {
        var request = new InstanceMethodCallTransactionRequest(
        		SignatureAlgorithms.ed25519().getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
        		StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                CodeSignature.GET_GAMETE,
                StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO)
        );

        String signature = toBase64(request.getSignature());
        assertEquals("xHZjYO3/yKgYum/cBXFrjNFe8uzS01Sf0EDWcqGNKnkLG+LvdcqbL7BrRgX0IqWq42rlhht6nl9ZwN3sPGfaDg==", signature);
    }

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) receive")
    public void testVoidInstanceMethodCallTransactionRequest() throws Exception {
        var transaction = TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882");
		var storageReference = StorageValues.reference(transaction, BigInteger.ZERO);

		var ed25519 = SignatureAlgorithms.ed25519();
		var request = new InstanceMethodCallTransactionRequest(
                ed25519.getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
                storageReference,
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                transaction,
                CodeSignature.RECEIVE_INT,
                storageReference,
                StorageValues.intOf(300)
        );

        String signature = toBase64(request.getSignature());
        assertEquals("FLCa/ygo2zc6gPcVt/px+uU//fK2d/4hosJhzA9B+ZxGlyXdIfV4hP1vqDTzpndKANOqGyZwfglCqv5fCr1ZBw==", signature);
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) nonce")
    public void testNonVoidStaticMethodCallTransactionRequest() throws Exception {
        var request = new StaticMethodCallTransactionRequest(
        		SignatureAlgorithms.ed25519().getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
        		StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                CodeSignature.NONCE
        );

        String signature = toBase64(request.getSignature());
        assertEquals("E6i4z6oj8/iUtHBRaLJyJ+w9erPqpy6B5Frtee2nWScfF00QOJCPGPMeXIRCUA8pw1s639dX3Mn0C0TlkYHsAQ==", signature);
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) receive")
    public void testVoidStaticMethodCallTransactionRequest() throws Exception {
        var request = new StaticMethodCallTransactionRequest(
        		SignatureAlgorithms.ed25519().getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
        		StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                CodeSignature.RECEIVE_INT,
                StorageValues.intOf(300)
        );

        String signature = toBase64(request.getSignature());
        assertEquals("/klFrbZfosMu3IRHpT351OuL1lyS+WnBh+puBlR70ryziPXrA88NbC8QVt0l+UPovE/7R+5eDx/7u07fX7tHCA==", signature);
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) balance of gasStation")
    public void testNonVoidStaticMethodCallTransactionGasStationRequest() throws Exception {
        var nonVoidMethodSignature = new NonVoidMethodSignature(
        		StorageTypes.GAS_STATION,
                "balance",
                StorageTypes.BIG_INTEGER,
                StorageTypes.STORAGE
        );

        var request = new StaticMethodCallTransactionRequest(
        		SignatureAlgorithms.ed25519().getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
        		StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                nonVoidMethodSignature,
                StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO)
        );

        String signature = toBase64(request.getSignature());
        assertEquals("2ayYF3hfqesk7ihTWbrzu3l2NBDKq1O10oVxfADuvmvQKbFROT/+6aXIDXmJaql67nPPi4PtMBCe1TJatD/3Ag==", signature);
    }

    @Test
    @DisplayName("new JarStoreTransactionRequest(..) lambdas jar")
    public void testJarStoreTransactionRequest() throws Exception {
        var request = new JarStoreTransactionRequest(
        		SignatureAlgorithms.ed25519().getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
        		StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                Marshallable.bytesOf("lambdas.jar")
        );

        String signature = toBase64(request.getSignature());
        assertEquals("ykpExl8XDss5Jy2h24g67vkdZJS4eEjbQy4OspyWnNCUVcCjvP9p/1WZu6l8dCmwq4BIh/HPN2dXWO5doE7VAA==", signature);
    }

    private static String toBase64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes));
    }
}