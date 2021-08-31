package io.hotmoka.tests;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests;

public class SignedRequests {

    @Test
    @DisplayName("new ConstructorCallTransactionRequest(..)")
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
        Assertions.assertEquals("t3FoY6hIU9epFgUeIWM3XVlOkpvBWFwcyojVvyWMNwFIN5gdZLFgKrqvxWPDMFFe5XuqGfooDTQK0DwTCZ39BQ==", signature);
    }

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) NonVoidMethod")
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
        Assertions.assertEquals("3x3ouGZVQc4z4adwy5SWetZTrQNXnFPhP2Tpy0MVj8gYkHu2tnmcgK3olkc82mFvJwiEghHYCLHBkRRfvk7aCQ==", signature);
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
                SignedTransactionRequest.Signer.with(SignatureAlgorithmForTransactionRequests.ed25519(), keys()),
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
        Assertions.assertEquals("wAa/VDhpJdLWmUR+fMXw//abHaxZaxqXfbi+5bsVgrET9qOMbzhEQoUYih53KalhgWgnmdxRiNUDl7QQwRNBBw==", signature);
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) NonVoidMethod")
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
        Assertions.assertEquals("nrqnb2FS9+LUZGe1cPPmKzISUnCoNCsktGfoi/cKzHr7XLX09vSG9xJ9G9qGSrYPbi4tcgjnYYP0HzO5wup9AQ==", signature);
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
        Assertions.assertEquals("8WkBx2+ncinZ1ocBDr2qg0D0WC+6PdkPGD2VLdNBrG7b6LX+O5S++tKM/WMhJyztUlgEXyab8VVwS4ywszZWBg==", signature);
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
        Assertions.assertEquals("f2+6p7e+k5UXgVKt6fNWzAIlpci6ua/HfX+lIQmC7rCXzYNsbMFYWHaA7ebYjqDTkZnOqoQkaDARZwJNNR5pAA==", signature);
    }

    @Test
    @Disabled
    @DisplayName("new JarStoreTransactionRequest(..)")
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
        Assertions.assertEquals("EWbpepg+tfhSIwyUblTyqEJOmYklH1wROTGijJ323bkifnxNE4teSLw7TTdM2D9WIUs5OtlYtHdRDh35RPNHDg==", signature);
    }

    private static KeyPair keys() throws NoSuchAlgorithmException {
    	return SignatureAlgorithmForTransactionRequests.ed25519().getKeyPair(new byte[16], "");
    }

    private static String toBase64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes));
    }
}
