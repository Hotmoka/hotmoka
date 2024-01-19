/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.tests;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.updates.ClassTag;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.network.values.TransactionReferenceModel;
import io.hotmoka.node.api.JarSupplier;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.remote.RemoteNodeConfigBuilders;
import io.hotmoka.node.remote.RemoteNodes;
import io.hotmoka.node.remote.api.RemoteNodeConfig;
import io.hotmoka.node.service.NodeServiceConfigBuilders;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.service.api.NodeServiceConfig;
import io.hotmoka.verification.VerificationException;

public class NodeFromNetworkWS extends HotmokaTest {
    private final ClassType HASH_MAP_TESTS = StorageTypes.classNamed("io.hotmoka.examples.javacollections.HashMapTests");
    private final NodeServiceConfig serviceConfig = NodeServiceConfigBuilders.defaults().setPort(8081).build();
    private final RemoteNodeConfig remoteNodeConfig = RemoteNodeConfigBuilders.defaults()
    	.usesWebSockets(true)
    	.setURL("localhost:8081").build();

    @BeforeEach
    void beforeEach() throws Exception {
        setAccounts(_1_000_000_000);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getTakamakaCode")
    void testRemoteGetTakamakaCode() throws Exception {
        TransactionReference localTakamakaCode = node.getTakamakaCode();
        TransactionReference remoteTakamakaCode;

        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
             remoteTakamakaCode = remote.getTakamakaCode();
        }

        assertEquals(localTakamakaCode, remoteTakamakaCode);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getSignatureAlgorithmForRequests")
    void testRemoteGetSignatureAlgorithmForRequests() throws Exception {
        SignatureAlgorithm algo;

        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            algo = SignatureAlgorithms.of(remote.getNameOfSignatureAlgorithmForRequests());
        }

        assertNotNull(algo);

        String algoName = algo.getClass().getName();
        assertTrue(algoName.endsWith("ED25519") || algoName.endsWith("ED25519DET") || algoName.endsWith("SHA256DSA")
        	|| algoName.endsWith("QTESLA1") || algoName.endsWith("QTESLA3") || algoName.endsWith("EMPTY"));
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getClassTag")
    void testRemoteGetClassTag() throws Exception {
        ClassTag localClassTag = node.getClassTag(account(0));
        ClassTag remoteClassTag;

        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            remoteClassTag = remote.getClassTag(account(0));
        }

        assertEquals(localClassTag, remoteClassTag);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getClassTag for a non-existing reference")
    void testRemoteGetClassTagNonExisting() {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            remote.getClassTag(getInexistentStorageReference());
        }
        catch (Exception e) {
            assertTrue(e instanceof NoSuchElementException);
            assertEquals("Unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", e.getMessage());
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getState")
    void testRemoteGetState() throws Exception {
        Stream<Update> localState = node.getState(account(0));
        Stream<Update> remoteState;

        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            remoteState = remote.getState(account(0));
        }

        assertEquals(localState.collect(Collectors.toSet()), remoteState.collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getState for a non-existing reference")
    void testRemoteGetStateNonExisting() {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            remote.getState(getInexistentStorageReference());
        }
        catch (Exception e) {
            assertTrue(e instanceof NoSuchElementException);
            assertEquals("Unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", e.getMessage());
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getRequest")
    void testRemoteGetRequest() throws Exception {
        TransactionRequest<?> request;

        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            request = remote.getRequest(node.getTakamakaCode());
        }

        // the jar containing the base Takamaka code was installed by an initial jar store transaction request
        assertTrue(request instanceof JarStoreInitialTransactionRequest);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getRequest for a non-existing reference")
    void testRemoteGetRequestNonExisting() throws Exception {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
        	var e = assertThrows(NoSuchElementException.class, () -> remote.getRequest(getInexistentTransactionReference()));
        	assertEquals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", e.getMessage());
        }
    }


    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse")
    void testRemoteGetResponse() throws Exception {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
        	TransactionResponse response = remote.getResponse(node.getTakamakaCode());
            // the jar containing the base Takamaka code was installed by an initial jar store transaction
            assertTrue(response instanceof JarStoreInitialTransactionResponse);
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse for a non-existing reference")
    void testRemoteGetResponseNonExisting() {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            remote.getResponse(getInexistentTransactionReference());
        }
        catch (Exception e) {
            assertTrue(e instanceof NoSuchElementException);
            assertEquals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", e.getMessage());
            return;
        }

        fail("expected exception");
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse for the reference of a failed request")
    void testRemoteGetResponseFailed() throws Exception {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
            // this means that the request fails and the future refers to a failed request; since this is a post,
            // the execution does not stop, nor throws anything
            JarSupplier future = postJarStoreTransaction(privateKey(0), account(0), BigInteger.valueOf(10_000), ONE, takamakaCode(), bytesOf("lambdas.jar")
                    // takamakaCode(), // <-- forgot that
            );

            // we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
            // since the request failed and its transaction was rejected
            try {
                future.get();
            }
            catch (TransactionRejectedException e) {
                // yes, we know
            }

            // if we ask for the outcome of the request, we will get the TransactionRejectedException as answer
            remote.getResponse(future.getReferenceOfRequest());
        }
        catch (TransactionRejectedException e) {
            assertTrue(e.getMessage().contains(ClassNotFoundException.class.getName()));
            return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getPolledResponse")
    void testRemoteGetPolledResponse() throws Exception {
        TransactionResponse response;

        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            // we install a jar in blockchain
            JarSupplier future = postJarStoreTransaction(privateKey(0), account(0), _500_000, ONE,
                    takamakaCode(), bytesOf("lambdas.jar"), takamakaCode());

            // we poll for its result
            response = remote.getPolledResponse(future.getReferenceOfRequest());
        }

        // lambdas.jar has been correctly installed in the node, hence the response is successful
        assertTrue(response instanceof JarStoreTransactionSuccessfulResponse);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getPolledResponse for a non-existing reference")
    void testRemoteGetPolledResponseNonExisting() {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            remote.getPolledResponse(getInexistentTransactionReference());
        }
        catch (Exception e) {
            assertTrue(e instanceof TimeoutException);
            assertTrue(e.getMessage().contains("cannot find the response of transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
            return;
        }

        fail("expected exception");
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getPolledResponse for the reference of a failed request")
    void testRemoteGetPolledResponseFailed() throws Exception {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
            // this means that the request fails and the future refers to a failed request; since this is a post,
            // the execution does not stop, nor throws anything
            JarSupplier future = postJarStoreTransaction(privateKey(0), account(0), BigInteger.valueOf(10_000), ONE, takamakaCode(), bytesOf("lambdas.jar")
                    // takamakaCode(), // <-- forgot that
            );

            // we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
            // since the request failed and its transaction was rejected
            try {
                future.get();
            }
            catch (TransactionRejectedException e) {
                // yes, we know
            }

            // if we ask for the outcome of the request, we will get the TransactionRejectedException as answer
            remote.getPolledResponse(future.getReferenceOfRequest());
        }
        catch (TransactionRejectedException e) {
            assertTrue(e.getMessage().contains(ClassNotFoundException.class.getName()));
            return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network service from a Hotmoka node and makes a remote call to addJarStoreTransaction")
    void testRemoteAddJarStoreTransaction() throws Exception {
        TransactionReference transaction;

        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            transaction = remote.addJarStoreTransaction(new JarStoreTransactionRequest
            	(signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
                 ZERO, chainId, _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar"), takamakaCode()));
        }

        assertNotNull(transaction);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to addJarStoreTransactionRequest for a request that gets rejected")
    void testRemoteAddJarStoreTransactionRejected() throws Exception {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
            // this means that the request fails and the future refers to a failed request; since this is a post,
            // the execution does not stop, nor throws anything
            remote.addJarStoreTransaction(new JarStoreTransactionRequest
                    (signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
                            ZERO, chainId, _50_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
                            // , takamakaCode() // <-- forgot that
                    ));
        }
        catch (TransactionRejectedException e) {
            assertTrue(e.getMessage().contains(ClassNotFoundException.class.getName()));
            return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to addJarStoreTransactionRequest for a request that fails")
    void testRemoteAddJarStoreTransactionFailed() throws Exception {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            remote.addJarStoreTransaction(new JarStoreTransactionRequest
                    (signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
                            ZERO, chainId, _100_000, ONE, takamakaCode(), bytesOf("callernotonthis.jar"), takamakaCode()));
        }
        catch (TransactionException e) {
            assertTrue(e.getMessage().contains(VerificationException.class.getName()));
            assertTrue(e.getMessage().contains("caller() can only be called on \"this\""));
            return;
        }

        fail("expected exception");
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to postJarStoreTransactionRequest")
    void testRemotePostJarStoreTransaction() throws Exception {
        TransactionReference transaction;

        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            JarSupplier future = remote.postJarStoreTransaction(new JarStoreTransactionRequest
                    (signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
                            ZERO, chainId, _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar"), takamakaCode()));

            // we wait until the request has been processed
            transaction = future.get();
        }

        assertNotNull(transaction);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to postJarStoreTransactionRequest for a request that gets rejected")
    void testRemotePostJarStoreTransactionRejected() throws Exception {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
            // this means that the request fails and the future refers to a failed request; since this is a post,
            // the execution does not stop, nor throws anything
            JarSupplier future = remote.postJarStoreTransaction(new JarStoreTransactionRequest
                    (signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
                            ZERO, chainId, _50_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
                            // , takamakaCode() // <-- forgot that
                    ));

            // we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
            // since the request failed and its transaction was rejected
            future.get();
        }
        catch (TransactionRejectedException e) {
            assertTrue(e.getMessage().contains(ClassNotFoundException.class.getName()));
            return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to postJarStoreTransactionRequest for a request that fails")
    void testRemotePostJarStoreTransactionFailed() throws Exception {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
            // this means that the request fails and the future refers to a failed request; since this is a post,
            // the execution does not stop, nor throws anything
            JarSupplier future = remote.postJarStoreTransaction(new JarStoreTransactionRequest
                    (signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
                            ZERO, chainId, _100_000, ONE, takamakaCode(), bytesOf("callernotonthis.jar"), takamakaCode()));

            // we wait until the request has been processed; this will throw a TransactionException at the end,
            // since the request was accepted but its execution failed
            future.get();
        }
        catch (TransactionException e) {
            assertTrue(e.getMessage().contains(VerificationException.class.getName()));
            assertTrue(e.getMessage().contains("caller() can only be called on \"this\""));
            return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to runStaticMethodCallTransaction")
    void testRemoteRunStaticMethodCallTransaction() throws Exception {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            TransactionReference jar = addJarStoreTransaction(privateKey(0), account(0),
            		_500_000, ONE, takamakaCode(), bytesOf("javacollections.jar"), takamakaCode());

            var toString = (StringValue) remote.runStaticMethodCallTransaction
            	(new StaticMethodCallTransactionRequest(account(0), _50_000, jar, MethodSignatures.of(HASH_MAP_TESTS, "testToString1", StorageTypes.STRING)));
            assertEquals("[how, are, hello, you, ?]", toString.getValue());
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to runInstanceMethodCallTransaction")
    void testRemoteRunInstanceMethodCallTransaction() throws Exception {
        try (var service = NodeServices.of(serviceConfig, node); var remote = RemoteNodes.of(remoteNodeConfig)) {
            var request = new InstanceMethodCallTransactionRequest
            	(account(0), _50_000, takamakaCode(), MethodSignatures.NONCE, account(0));

            BigIntegerValue value = (BigIntegerValue) remote.runInstanceMethodCallTransaction(request);
            assertEquals(ZERO, value.getValue());
        }
    }

    private static TransactionReference getInexistentTransactionReference() {
        var reference = new JsonObject();
        // we use a non-existent hash
        reference.addProperty("hash", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        reference.addProperty("type", "local");

        return new GsonBuilder().disableHtmlEscaping().create().fromJson(reference, TransactionReferenceModel.class).toBean();
    }

    private static StorageReference getInexistentStorageReference() {
        return StorageValues.reference(getInexistentTransactionReference(), BigInteger.valueOf(42));
    }
}
