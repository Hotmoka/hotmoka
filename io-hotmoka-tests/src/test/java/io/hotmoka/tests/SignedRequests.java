package io.hotmoka.tests;

import java.math.BigInteger;
import java.security.KeyPair;
import java.util.Base64;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.SignatureAlgorithmForTransactionRequests;

public class SignedRequests {
    private static final KeyPair keyPair;

    static {
        try {
            keyPair = SignatureAlgorithmForTransactionRequests.ed25519().getKeyPair(hexToBytes("64ea6e847fd7c3c5403871f9e57d9f48"), "mysecret");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    @DisplayName("new ConstructorCallTransactionRequest(..) manifest")
    public void testConstructorCallTransactionRequest() throws Exception {

        ConstructorSignature constructorSignature = new ConstructorSignature(
                ClassType.MANIFEST,
                ClassType.BIG_INTEGER
        );

        ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.ed25519(), keys()),
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(11500),
                BigInteger.valueOf(500),
                new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                constructorSignature,
                new BigIntegerValue(BigInteger.valueOf(999))
        );

        String signature = toBase64(request.getSignature());
        Assertions.assertEquals("8xMW6vu9SYIFmZkGq906s7rvsclXTTJmA2mpc7QTf9Tgz7WeuJMByAX5ihBKbq5m8dmR9PD/Uv7ALmSL0iJGDg==", signature);
    }

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) getGamete")
    public void testNonVoidInstanceMethodCallTransactionRequest() throws Exception {

        InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.ed25519(), keys()),
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
        Assertions.assertEquals("xHZjYO3/yKgYum/cBXFrjNFe8uzS01Sf0EDWcqGNKnkLG+LvdcqbL7BrRgX0IqWq42rlhht6nl9ZwN3sPGfaDg==", signature);
    }

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) receive")
    public void testVoidInstanceMethodCallTransactionRequest() throws Exception {

        MethodSignature receiveInt = new VoidMethodSignature(
                ClassType.PAYABLE_CONTRACT,
                "receive",
                BasicTypes.INT
        );

        LocalTransactionReference transaction = new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882");
		StorageReference storageReference = new StorageReference(transaction, BigInteger.ZERO);

		InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.ed25519(), keys()),
                storageReference,
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                transaction,
                receiveInt,
                storageReference,
                new IntValue(300)
        );

        String signature = toBase64(request.getSignature());
        Assertions.assertEquals("FLCa/ygo2zc6gPcVt/px+uU//fK2d/4hosJhzA9B+ZxGlyXdIfV4hP1vqDTzpndKANOqGyZwfglCqv5fCr1ZBw==", signature);
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) nonce")
    public void testNonVoidStaticMethodCallTransactionRequest() throws Exception {

        StaticMethodCallTransactionRequest request = new StaticMethodCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.ed25519(), keys()),
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                CodeSignature.NONCE
        );

        String signature = toBase64(request.getSignature());
        Assertions.assertEquals("E6i4z6oj8/iUtHBRaLJyJ+w9erPqpy6B5Frtee2nWScfF00QOJCPGPMeXIRCUA8pw1s639dX3Mn0C0TlkYHsAQ==", signature);
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) receive")
    public void testVoidStaticMethodCallTransactionRequest() throws Exception {

        MethodSignature receiveInt = new VoidMethodSignature(
                ClassType.PAYABLE_CONTRACT,
                "receive",
                BasicTypes.INT
        );

        StaticMethodCallTransactionRequest request = new StaticMethodCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.ed25519(), keys()),
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                receiveInt,
                new IntValue(300)
        );

        String signature = toBase64(request.getSignature());
        Assertions.assertEquals("/klFrbZfosMu3IRHpT351OuL1lyS+WnBh+puBlR70ryziPXrA88NbC8QVt0l+UPovE/7R+5eDx/7u07fX7tHCA==", signature);
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) balance of gasStation")
    public void testNonVoidStaticMethodCallTransactionGasStationRequest() throws Exception {

        NonVoidMethodSignature nonVoidMethodSignature = new NonVoidMethodSignature(
                ClassType.GAS_STATION,
                "balance",
                ClassType.BIG_INTEGER,
                ClassType.STORAGE
        );

        StaticMethodCallTransactionRequest request = new StaticMethodCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.ed25519(), keys()),
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                nonVoidMethodSignature,
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO)
        );

        String signature = toBase64(request.getSignature());
        Assertions.assertEquals("2ayYF3hfqesk7ihTWbrzu3l2NBDKq1O10oVxfADuvmvQKbFROT/+6aXIDXmJaql67nPPi4PtMBCe1TJatD/3Ag==", signature);
    }

    @Test
    @DisplayName("new JarStoreTransactionRequest(..) lambdas jar")
    public void testJarStoreTransactionReqest() throws Exception {

        JarStoreTransactionRequest request = new JarStoreTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.ed25519(), keys()),
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                Marshallable.bytesOf("lambdas.jar")
        );

        String signature = toBase64(request.getSignature());
        Assertions.assertEquals("p8eOIXuYQEv/K7qvgxRacUhwUIBo4XWPrHHc0G6Fv9dnlniuJY+sbNZOoAglB14QOASOD/q1rTSjiYRiOQvWCw==", signature);
    }

    private static KeyPair keys() {
    	return keyPair;
    }

    private static byte[] hexToBytes(String hex) {
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            int val = Integer.parseInt(hex.substring(index, index + 2), 16);
            result[i] = (byte)val;
        }
        return result;
    }

    private static String toBase64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes));
    }
}
