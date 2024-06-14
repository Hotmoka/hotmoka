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

import java.math.BigInteger;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.JarFuture;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.remote.RemoteNodes;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.verification.VerificationException;

public class NodeFromNetwork extends HotmokaTest {
    private final static ClassType HASH_MAP_TESTS = StorageTypes.classNamed("io.hotmoka.examples.javacollections.HashMapTests");
    private final static TransactionReference INEXISTENT_TRANSACTION_REFERENCE = TransactionReferences.of("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    private final static StorageReference INEXISTENT_STORAGE_REFERENCE = StorageValues.reference(INEXISTENT_TRANSACTION_REFERENCE, BigInteger.valueOf(42));
    private final static int PORT = 8002;
    private final static URI URI = java.net.URI.create("ws://localhost:" + PORT);

    @BeforeEach
    void beforeEach() throws Exception {
        setAccounts(_1_000_000_000);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getTakamakaCode")
    void testRemoteGetTakamakaCode() throws Exception {
    	TransactionReference localTakamakaCode = takamakaCode();
    	TransactionReference remoteTakamakaCode;

        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	remoteTakamakaCode = remote.getTakamakaCode();
        }

        assertEquals(localTakamakaCode, remoteTakamakaCode);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getSignatureAlgorithmForRequests")
    void testRemoteGetSignatureAlgorithmForRequests() throws Exception {
    	SignatureAlgorithm algo;

        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	algo = remote.getConfig().getSignatureForRequests();
        }

        assertNotNull(algo);
        String algoName = algo.getClass().getName();
        assertTrue(algoName.endsWith("ED25519") || algoName.endsWith("ED25519DET") || algoName.endsWith("SHA256DSA")
        	|| algoName.endsWith("QTESLA1") || algoName.endsWith("QTESLA3") || algoName.endsWith("EMPTY"));
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getClassTag")
    void testRemoteGetClassTag() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	assertEquals(node.getClassTag(account(0)), remote.getClassTag(account(0)));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getClassTag for a non-existing reference")
    void testRemoteGetClassTagNonExisting() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	assertThrows(UnknownReferenceException.class, () -> remote.getClassTag(INEXISTENT_STORAGE_REFERENCE));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getState")
    void testRemoteGetState() throws Exception {
    	Stream<Update> localState = node.getState(account(0));
        Stream<Update> remoteState;

        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	remoteState = remote.getState(account(0));
        }

        assertEquals(localState.collect(Collectors.toSet()), remoteState.collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getState with a non-existing reference")
    void testRemoteGetStateNonExisting() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	assertThrows(UnknownReferenceException.class, () -> remote.getState(INEXISTENT_STORAGE_REFERENCE));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getRequest")
    void testRemoteGetRequest() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	// the jar containing the base Takamaka code was installed by an initial jar store transaction request
        	assertTrue(remote.getRequest(takamakaCode()) instanceof JarStoreInitialTransactionRequest);
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getRequest for a non-existing reference")
    void testRemoteGetRequestNonExisting() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	assertThrows(UnknownReferenceException.class, () -> remote.getRequest(INEXISTENT_TRANSACTION_REFERENCE));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse")
    void testRemoteGetResponse() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	// the jar containing the base Takamaka code was installed by an initial jar store transaction
        	assertTrue(remote.getResponse(takamakaCode()) instanceof JarStoreInitialTransactionResponse);
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse for a non-existing reference")
    void testRemoteGetResponseNonExisting() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	assertThrows(UnknownReferenceException.class, () -> remote.getResponse(INEXISTENT_TRANSACTION_REFERENCE));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse for the reference of a failed request")
    void testRemoteGetResponseFailed() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	// we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
        	// this means that the request fails and the future refers to a failed request; since this is a post,
        	// the execution does not stop, nor throws anything
        	JarFuture future = postJarStoreTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
        		// takamakaCode(), // <-- forgot that
        	);

        	// we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
        	// since the request failed and its transaction was rejected
        	assertThrows(TransactionRejectedException.class, future::get);

        	// if we ask for the outcome of the request, we will get the TransactionRejectedException as answer
        	TransactionRejectedException e = assertThrows(TransactionRejectedException.class, () -> remote.getPolledResponse(future.getReferenceOfRequest()));
        	assertTrue(e.getMessage().contains(ClassNotFoundException.class.getName()));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getPolledResponse")
    void testRemoteGetPolledResponse() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	// we install a jar in blockchain
        	JarFuture future = postJarStoreTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar"), takamakaCode());

        	// we poll for its result: lambdas.jar has been correctly installed in the node, hence the response is successful
        	assertTrue(remote.getPolledResponse(future.getReferenceOfRequest()) instanceof JarStoreTransactionSuccessfulResponse);
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getPolledResponse for the reference of a failed request")
    void testRemoteGetPolledResponseFailed() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	// we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
        	// this means that the request fails and the future refers to a failed request; since this is a post,
        	// the execution does not stop, nor throws anything
        	JarFuture future = postJarStoreTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
        		// takamakaCode(), // <-- forgot that
       		);

        	// we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
        	// since the request failed and its transaction was rejected
        	assertThrows(TransactionRejectedException.class, future::get);

        	// if we ask for the outcome of the request to the remote node, we will get the TransactionRejectedException as answer
        	TransactionRejectedException e = assertThrows(TransactionRejectedException.class, () -> remote.getPolledResponse(future.getReferenceOfRequest()));
        	assertTrue(e.getMessage().contains(ClassNotFoundException.class.getName()));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to addJarStoreTransaction")
    void testRemoteAddJarStoreTransaction() throws Exception {
    	try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
    		assertNotNull(remote.addJarStoreTransaction(TransactionRequests.jarStore
       			(signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
				ZERO, chainId(), _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar"), takamakaCode())));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to addJarStoreTransactionRequest for a request that gets rejected")
    void testRemoteAddJarStoreTransactionRejected() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	// we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
        	// this means that the request fails and the future refers to a failed request; since this is a post,
        	// the execution does not stop, nor throws anything
        	TransactionRejectedException e = assertThrows(TransactionRejectedException.class, () ->
        		remote.addJarStoreTransaction(TransactionRequests.jarStore
       				(signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
					ZERO, chainId(), _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
					// , takamakaCode() // <-- forgot that
			)));
        	assertTrue(e.getMessage().contains(ClassNotFoundException.class.getName()));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to addJarStoreTransactionRequest for a request that fails")
    void testRemoteAddJarStoreTransactionFailed() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	TransactionException e = assertThrows(TransactionException.class, () ->
        		remote.addJarStoreTransaction(TransactionRequests.jarStore
        			(signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
        			ZERO, chainId(), _100_000, ONE, takamakaCode(), bytesOf("callernotonthis.jar"), takamakaCode())));

        	assertTrue(e.getMessage().contains(VerificationException.class.getName()));
        	assertTrue(e.getMessage().contains("caller() can only be called on \"this\""));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to postJarStoreTransactionRequest")
    void testRemotePostJarStoreTransaction() throws Exception {
    	try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
    		JarFuture future = remote.postJarStoreTransaction(TransactionRequests.jarStore
           			(signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
    				ZERO, chainId(), _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar"), takamakaCode()));

        	// we wait until the request has been processed
    		assertNotNull(future.get());
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to postJarStoreTransactionRequest for a request that gets rejected")
    void testRemotePostJarStoreTransactionRejected() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	// we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
        	// this means that the request fails and the future refers to a failed request; since this is a post,
        	// the execution does not stop, nor throws anything
        	JarFuture future = remote.postJarStoreTransaction(TransactionRequests.jarStore
           		(signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
    			ZERO, chainId(), _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
        		// , takamakaCode() // <-- forgot that
           	));

        	// we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
        	// since the request failed and its transaction was rejected
        	TransactionRejectedException e = assertThrows(TransactionRejectedException.class, future::get);
        	assertTrue(e.getMessage().contains(ClassNotFoundException.class.getName()));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to postJarStoreTransactionRequest for a request that fails")
    void testRemotePostJarStoreTransactionFailed() throws Exception {
        try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
        	// we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
        	// this means that the request fails and the future refers to a failed request; since this is a post,
        	// the execution does not stop, nor throws anything
        	JarFuture future = remote.postJarStoreTransaction(TransactionRequests.jarStore
           		(signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0),
       			ZERO, chainId(), _500_000, ONE, takamakaCode(), bytesOf("callernotonthis.jar"), takamakaCode()));

        	// we wait until the request has been processed; this will throw a TransactionException at the end,
        	// since the request was accepted but its execution failed
        	TransactionException e = assertThrows(TransactionException.class, future::get);
        	assertTrue(e.getMessage().contains(VerificationException.class.getName()));
        	assertTrue(e.getMessage().contains("caller() can only be called on \"this\""));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to runStaticMethodCallTransaction")
    void testRemoteRunStaticMethodCallTransaction() throws Exception {
    	try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
    		TransactionReference jar = addJarStoreTransaction(privateKey(0), account(0),
    			_500_000, ONE, takamakaCode(), bytesOf("javacollections.jar"), takamakaCode());

    		var toString = (StringValue) remote.runStaticMethodCallTransaction
       			(TransactionRequests.staticViewMethodCall(account(0), _100_000, jar, MethodSignatures.ofNonVoid(HASH_MAP_TESTS, "testToString1", StorageTypes.STRING))).get();

    		assertEquals("[how, are, hello, you, ?]", toString.getValue());
    	}
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to runInstanceMethodCallTransaction")
    void testRemoteRunInstanceMethodCallTransaction() throws Exception {
    	try (var service = NodeServices.of(node, PORT); var remote = RemoteNodes.of(URI, 10_000)) {
			var request = TransactionRequests.instanceViewMethodCall(account(0), _100_000, takamakaCode(), MethodSignatures.NONCE, account(0));
			assertEquals(ZERO, remote.runInstanceMethodCallTransaction(request).get().asBigInteger(__ -> new ClassCastException()));
        }
    }
}