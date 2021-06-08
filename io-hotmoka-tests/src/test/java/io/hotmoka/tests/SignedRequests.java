package io.hotmoka.tests;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
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
    private static final KeyPair keyPair;

    static {
        keyPair = loadKeys();
    }


    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) NonVoidMethod = M93FSC2jthDeWkcEDNFMnl7G88i9MXeTJfH0R2nzaHfodQNwqm9udQ2aPpZfKMNSQ1y2EvS1w0Eo1GcEzD2rDQ==")
    public void testNonVoidInstanceMethodCallTransactionRequest() throws Exception {

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

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) VoidMethod = daUNn5acSc8knsDGOHD11EO1Mrai6rvJpd/F2TBlwxcCrzqJg49G8HqG4dKoU9dxHOkeIh2anCNG5Fdx5VOJDA==")
    public void testVoidInstanceMethodCallTransactionRequest() throws Exception {

        MethodSignature receiveInt = new VoidMethodSignature(
                ClassType.PAYABLE_CONTRACT,
                "receive",
                BasicTypes.INT
        );

        InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.mk("ed25519"), keyPair),
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                receiveInt,
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                new IntValue(300)
        );

        String signature = toBase64(request.getSignature());
        Assertions.assertEquals("daUNn5acSc8knsDGOHD11EO1Mrai6rvJpd/F2TBlwxcCrzqJg49G8HqG4dKoU9dxHOkeIh2anCNG5Fdx5VOJDA==", signature);
    }


    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) NonVoidMethod = kGANXzM4vsYs/HPhFb+NdfAY10clY/qHbnX3C0oIvip1w8zKXMeymLsl3XLsXbpEsRR5IvfOppIL6B8sa4qhCw==")
    public void testNonVoidStaticMethodCallTransactionRequest() throws Exception {

        StaticMethodCallTransactionRequest request = new StaticMethodCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.mk("ed25519"), keyPair),
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                CodeSignature.NONCE
        );

        String signature = toBase64(request.getSignature());
        Assertions.assertEquals("kGANXzM4vsYs/HPhFb+NdfAY10clY/qHbnX3C0oIvip1w8zKXMeymLsl3XLsXbpEsRR5IvfOppIL6B8sa4qhCw==", signature);
    }


    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) VoidMethod = Kfrc1vksJppDGsNBOC1dmhm6QGr9g5CaY9mjplBHzmBxplbUWsFQ7Aif6oWJ76ROIq41f0UNltKlxQJ8uYi9BA==")
    public void testVoidStaticMethodCallTransactionRequest() throws Exception {

        MethodSignature receiveInt = new VoidMethodSignature(
                ClassType.PAYABLE_CONTRACT,
                "receive",
                BasicTypes.INT
        );

        StaticMethodCallTransactionRequest request = new StaticMethodCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.mk("ed25519"), keyPair),
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
        Assertions.assertEquals("Kfrc1vksJppDGsNBOC1dmhm6QGr9g5CaY9mjplBHzmBxplbUWsFQ7Aif6oWJ76ROIq41f0UNltKlxQJ8uYi9BA==", signature);
    }


    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) NonVoid = Pq+zM4j1WeB/eiUGohYrHuz8ZdXYTPIJrdBn/hEvrxyukvd1onqeXbyVF5CXcXvoEP1NjleEhxLHItjFsd/4BA==")
    public void testNonVoidStaticMethodCallTransactionGasStationRequest() throws Exception {

        NonVoidMethodSignature nonVoidMethodSignature = new NonVoidMethodSignature(
                ClassType.GAS_STATION,
                "balance",
                ClassType.BIG_INTEGER,
                ClassType.STORAGE
        );

        StaticMethodCallTransactionRequest request = new StaticMethodCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.mk("ed25519"), keyPair),
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
        Assertions.assertEquals("Pq+zM4j1WeB/eiUGohYrHuz8ZdXYTPIJrdBn/hEvrxyukvd1onqeXbyVF5CXcXvoEP1NjleEhxLHItjFsd/4BA==", signature);
    }

    @Test
    @DisplayName("new JarStoreTransactionRequest(..) = Eb/8lnQZ7C5mPVJAq9wYFjz59VPp9MR05XpRLcubNw1GIKJw+NTAreGdUD3JFCDv8lZCrDzKW3iiCRGWGRq4BA==")
    public void testJarStoreTransactionReqest() throws Exception {

        JarStoreTransactionRequest request = new JarStoreTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.mk("ed25519"), keyPair),
                new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                BigInteger.ONE,
                "chaintest",
                BigInteger.valueOf(5000),
                BigInteger.valueOf(4000),
                new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                Marshallable.bytesOf("lambdas.jar")
        );

        String signature = toBase64(request.getSignature());
        Assertions.assertEquals("Eb/8lnQZ7C5mPVJAq9wYFjz59VPp9MR05XpRLcubNw1GIKJw+NTAreGdUD3JFCDv8lZCrDzKW3iiCRGWGRq4BA==", signature);
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
