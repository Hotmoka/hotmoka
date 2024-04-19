package io.hotmoka.tests;

import static io.hotmoka.crypto.Base64.toBase64String;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.api.requests.SignedTransactionRequest;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;

public class SignedRequests {
    private static KeyPair keys;

    @BeforeAll
    public static void beforeAll() throws NoSuchAlgorithmException, HexConversionException {
    	keys = SignatureAlgorithms.ed25519().getKeyPair(Hex.fromHexString("64ea6e847fd7c3c5403871f9e57d9f48"), "mysecret");
    }

    @Test
    @DisplayName("new ConstructorCallTransactionRequest(..) manifest")
    public void testConstructorCallTransactionRequest() throws Exception {
        var constructorSignature = ConstructorSignatures.of(StorageTypes.MANIFEST, StorageTypes.BIG_INTEGER);

        var request = TransactionRequests.constructorCall(
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

        String signature = toBase64String(request.getSignature());
        assertEquals("6QJdhtgsC/YSdy7wSjKUKaQ9CqsY0QgSGt7HXTvyeiTD56vughXdSmx1DNGSmqxwBpLkqrPX6jzDzpAJwsSSAw==", signature);
    }

    @Test
    @DisplayName("TransactionRequests.instanceMethodCall(..) getGamete")
    public void testNonVoidInstanceMethodCallTransactionRequest() throws Exception {
        var request = TransactionRequests.instanceMethodCall(
        		SignatureAlgorithms.ed25519().getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
        		StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                MethodSignatures.GET_GAMETE,
                StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO)
        );

        String signature = toBase64String(request.getSignature());
        assertEquals("6h/4ZNDDOv93EOBxswheealsDLtVo9GYgkNEeeKNMny57vphpcenJUJepgRe9ecD6WKROT3LtJAOAKte0P5uDA==", signature);
    }

    @Test
    @DisplayName("TransactionRequests.instanceMethodCall(..) receive")
    public void testVoidInstanceMethodCallTransactionRequest() throws Exception {
        var transaction = TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882");
		var storageReference = StorageValues.reference(transaction, BigInteger.ZERO);

		var ed25519 = SignatureAlgorithms.ed25519();
		var request = TransactionRequests.instanceMethodCall(
                ed25519.getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
                storageReference,
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                transaction,
                MethodSignatures.RECEIVE_INT,
                storageReference,
                StorageValues.intOf(300)
        );

        String signature = toBase64String(request.getSignature());
        assertEquals("FLCa/ygo2zc6gPcVt/px+uU//fK2d/4hosJhzA9B+ZxGlyXdIfV4hP1vqDTzpndKANOqGyZwfglCqv5fCr1ZBw==", signature);
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) nonce")
    public void testNonVoidStaticMethodCallTransactionRequest() throws Exception {
        var request = TransactionRequests.staticMethodCall(
        		SignatureAlgorithms.ed25519().getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
        		StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                MethodSignatures.NONCE
        );

        String signature = toBase64String(request.getSignature());
        assertEquals("8VRzrSRA4H+H79YeKTbXw76ESnjOV4DSDkJraqIssmD4XmHcu+q/yu/MyHEtgF9Hw6yyg3lNTc/U6xq39PtqDA==", signature);
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) receive")
    public void testVoidStaticMethodCallTransactionRequest() throws Exception {
        var request = TransactionRequests.staticMethodCall(
        		SignatureAlgorithms.ed25519().getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
        		StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                MethodSignatures.RECEIVE_INT,
                StorageValues.intOf(300)
        );

        String signature = toBase64String(request.getSignature());
        assertEquals("BRV/S7Ra4m8I7FoDn1DU/aAOCjCOD0ScewTIhFtSEODo7mSEFxrE2+DSJQ10ipK/k6QLQEVGvnE79p0v7ijiBw==", signature);
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) balance of gasStation")
    public void testNonVoidStaticMethodCallTransactionGasStationRequest() throws Exception {
        var nonVoidMethodSignature = MethodSignatures.of(
        		StorageTypes.GAS_STATION,
                "balance",
                StorageTypes.BIG_INTEGER,
                StorageTypes.STORAGE
        );

        var request = TransactionRequests.staticMethodCall(
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

        String signature = toBase64String(request.getSignature());
        assertEquals("wYcJfA/sT8qLBvctYu0jm0xst5XFTKr7/KbLhPLyuyzwaDFIjTwgC3VW+viGpexOZmZutTjYbLqB3dywulU+Dg==", signature);
    }

    @Test
    @DisplayName("new JarStoreTransactionRequest(..) lambdas jar")
    public void testJarStoreTransactionRequest() throws Exception {
        var request = TransactionRequests.jarStore(
        		SignatureAlgorithms.ed25519().getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
        		StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                bytesOf("lambdas.jar")
        );

        String signature = toBase64String(request.getSignature());
        assertEquals("ykpExl8XDss5Jy2h24g67vkdZJS4eEjbQy4OspyWnNCUVcCjvP9p/1WZu6l8dCmwq4BIh/HPN2dXWO5doE7VAA==", signature);
    }

    private static byte[] bytesOf(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get("./jars/" + fileName));
    }
}