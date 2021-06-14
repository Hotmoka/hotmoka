package io.hotmoka.tests;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.requests.*;
import io.hotmoka.beans.signatures.*;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
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
    @DisplayName("new ConstructorCallTransactionRequest(..)")
    public void testConstructorCallTransactionRequest() throws Exception {

        ConstructorSignature constructorSignature = new ConstructorSignature(
                ClassType.MANIFEST,
                ClassType.BIG_INTEGER
        );

        ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest(
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.mk("ed25519"), keyPair),
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
        Assertions.assertEquals("P9PDt2/BL/pBVcFVwf9LvsTyb65O0SRzNC8ZeAe9Zbmn4AqTYJcFdltrWBYOFSej2I/TU3ejQyqKpPfCfp/vDA==", signature);
    }

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) NonVoidMethod")
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
        Assertions.assertEquals("vqn3qcfi3MMgJiSmLKcAtCJuX3bna3Qa+rgIku0owEhT3GaA7WwojtthtcmRKVuFj1wV+fVdgweqjNTH+FxDAg==", signature);
    }

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) VoidMethod")
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
        Assertions.assertEquals("8G7sgR0yhpRyS4dZc0sDiMRZZIkCh8m1eoFChSxWo5lL8SPtuxtoBLw4gwbN9dGLCUfqk3DpqUf5S0bwtVdMAA==", signature);
    }


    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) NonVoidMethod")
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
        Assertions.assertEquals("Q4oCMaptE+bLL5p5p+Uei6uINJ3TuB4/k3miqwjviKQ5ki0/oJ6hJI3xulbhaAhT5AV15P6Zy1XI2SjF9pPbDg==", signature);
    }


    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) VoidMethod")
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
        Assertions.assertEquals("0NwNhHIZCf3TFj5OQupJruLlRGsiR91uPhUsHTpxADgTYgIGJTULSoUAYRak1WNBUBMDG8Icx2xz3gnzhgDzBA==", signature);
    }


    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) NonVoid")
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
        Assertions.assertEquals("gNJAD15DKlQeMqqgPAKQv2jx2V7loNfkLIvJhpvUeYIxrImlyk6OmdLaAb49bHrNYY2MJoI/ujd9SBDvbK7HAA==", signature);
    }

    @Test
    @DisplayName("new JarStoreTransactionRequest(..)")
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
        Assertions.assertEquals("n5KOY/VWbm5fcUP7qYnJogfaxanj2997EJSpREKBDXOG+PC2FXllXttYl0pHtlDUJ41JzqEJ9KkKsBVTC7kZAA==", signature);
    }


    protected static KeyPair loadKeys() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Paths.get("gameteED25519.keys").toFile()))) {
            return (KeyPair) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static String toBase64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes));
    }
}
