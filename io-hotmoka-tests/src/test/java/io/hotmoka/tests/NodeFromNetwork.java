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

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.network.values.TransactionReferenceModel;
import io.hotmoka.node.api.JarSupplier;
import io.hotmoka.remote.RemoteNode;
import io.hotmoka.remote.RemoteNodeConfig;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import io.hotmoka.verification.VerificationException;

public class NodeFromNetwork extends HotmokaTest {
    private final ClassType HASH_MAP_TESTS = new ClassType("io.hotmoka.examples.javacollections.HashMapTests");
    private final NodeServiceConfig serviceConfig = new NodeServiceConfig.Builder().setPort(8081).setSpringBannerModeOn(false).build();
    private final RemoteNodeConfig remoteNodeconfig = new RemoteNodeConfig.Builder().setURL("localhost:8081").build();

    @BeforeEach
    void beforeEach() throws Exception {
        setAccounts(_1_000_000_000);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getTakamakaCode")
    void testRemoteGetTakamakaCode() throws Exception {
    	TransactionReference localTakamakaCode = node.getTakamakaCode();
    	TransactionReference remoteTakamakaCode;

        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	remoteTakamakaCode = remoteNode.getTakamakaCode();
        }

        assertEquals(localTakamakaCode, remoteTakamakaCode);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getSignatureAlgorithmForRequests")
    void testRemoteGetSignatureAlgorithmForRequests() throws Exception {
    	SignatureAlgorithm<SignedTransactionRequest> algo;

        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	algo = SignatureAlgorithms.of(remoteNode.getNameOfSignatureAlgorithmForRequests(), SignedTransactionRequest::toByteArrayWithoutSignature);
        }

        assertNotNull(algo);
        assertTrue(algo.getClass().getName().startsWith("io.hotmoka.crypto.internal."));
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getClassTag")
    void testRemoteGetClassTag() throws Exception {
    	ClassTag localClassTag = node.getClassTag(account(0));
        ClassTag remoteClassTag;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, node);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

        	remoteClassTag = remoteNode.getClassTag(account(0));
        }

        assertEquals(localClassTag, remoteClassTag);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getClassTag for a non-existing reference")
    void testRemoteGetClassTagNonExisting() {
        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	remoteNode.getClassTag(getInexistentStorageReference());
        }
        catch (Exception e) {
        	assertTrue(e instanceof NoSuchElementException);
            assertEquals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", e.getMessage());
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getState")
    void testRemoteGetState() throws Exception {
    	Stream<Update> localState = node.getState(account(0));
        Stream<Update> remoteState;

        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	remoteState = remoteNode.getState(account(0));
        }

        assertEquals(localState.collect(Collectors.toSet()), remoteState.collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getState for a non-existing reference")
    void testRemoteGetStateNonExisting() {
        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	remoteNode.getState(getInexistentStorageReference());
        }
        catch (Exception e) {
        	assertTrue(e instanceof NoSuchElementException);
            assertEquals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", e.getMessage());
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getRequest")
    void testRemoteGetRequest() throws Exception {
    	TransactionRequest<?> request;

        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	request = remoteNode.getRequest(node.getTakamakaCode());
        }

        // the jar containing the base Takamaka code was installed by an initial jar store transaction request
    	assertTrue(request instanceof JarStoreInitialTransactionRequest);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getRequest for a non-existing reference")
    void testRemoteGetRequestNonExisting() {
        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	remoteNode.getRequest(getInexistentTransactionReference());
        }
        catch (Exception e) {
        	assertTrue(e instanceof NoSuchElementException);
            assertEquals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", e.getMessage());
        	return;
        }

    	fail("expected exception");
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse")
    void testRemoteGetResponse() throws Exception {
    	TransactionResponse response;

        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	response = remoteNode.getResponse(node.getTakamakaCode());
        }

        // the jar containing the base Takamaka code was installed by an initial jar store transaction
    	assertTrue(response instanceof JarStoreInitialTransactionResponse);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse for a non-existing reference")
    void testRemoteGetResponseNonExisting() {
        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	remoteNode.getResponse(getInexistentTransactionReference());
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
        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	// we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
        	// this means that the request fails and the future refers to a failed request; since this is a post,
        	// the execution does not stop, nor throws anything
        	JarSupplier future = postJarStoreTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
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
        	remoteNode.getResponse(future.getReferenceOfRequest());
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

        try (NodeService nodeRestService = NodeService.of(serviceConfig, node);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

        	// we install a jar in blockchain
        	JarSupplier future = postJarStoreTransaction(privateKey(0), account(0), _500_000, ONE,
        		takamakaCode(), bytesOf("lambdas.jar"), takamakaCode());

        	// we poll for its result
        	response = remoteNode.getPolledResponse(future.getReferenceOfRequest());
        }

        // lambdas.jar has been correctly installed in the node, hence the response is successful
    	assertTrue(response instanceof JarStoreTransactionSuccessfulResponse);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getPolledResponse for a non-existing reference")
    void testRemoteGetPolledResponseNonExisting() {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, node);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

        	remoteNode.getPolledResponse(getInexistentTransactionReference());
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
        try (NodeService nodeRestService = NodeService.of(serviceConfig, node);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

        	// we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
        	// this means that the request fails and the future refers to a failed request; since this is a post,
        	// the execution does not stop, nor throws anything
        	JarSupplier future = postJarStoreTransaction(privateKey(0), account(0), _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
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

        	// if we ask for the outcome of the request to the remote node, we will get the TransactionRejectedException as answer
        	remoteNode.getPolledResponse(future.getReferenceOfRequest());
        }
        catch (TransactionRejectedException e) {
        	assertTrue(e.getMessage().contains(ClassNotFoundException.class.getName()));
        	return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to addJarStoreTransaction")
    void testRemoteAddJarStoreTransaction() throws Exception {
    	TransactionReference transaction;

    	try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
    		transaction = remoteNode.addJarStoreTransaction(new JarStoreTransactionRequest
       			(signature().getSigner(privateKey(0)), account(0),
				ZERO, chainId, _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar"), takamakaCode()));
        }

    	assertNotNull(transaction);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to addJarStoreTransactionRequest for a request that gets rejected")
    void testRemoteAddJarStoreTransactionRejected() throws Exception {
        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	// we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
        	// this means that the request fails and the future refers to a failed request; since this is a post,
        	// the execution does not stop, nor throws anything
        	remoteNode.addJarStoreTransaction(new JarStoreTransactionRequest
           		(signature().getSigner(privateKey(0)), account(0),
    			ZERO, chainId, _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
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
        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
            remoteNode.addJarStoreTransaction(new JarStoreTransactionRequest
       			(signature().getSigner(privateKey(0)), account(0),
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

    	try (NodeService nodeRestService = NodeService.of(serviceConfig, node);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

    		JarSupplier future = remoteNode.postJarStoreTransaction(new JarStoreTransactionRequest
           			(signature().getSigner(privateKey(0)), account(0),
    				ZERO, chainId, _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar"), takamakaCode()));

        	// we wait until the request has been processed
        	transaction = future.get();
        }

    	assertNotNull(transaction);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to postJarStoreTransactionRequest for a request that gets rejected")
    void testRemotePostJarStoreTransactionRejected() throws Exception {
        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	// we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
        	// this means that the request fails and the future refers to a failed request; since this is a post,
        	// the execution does not stop, nor throws anything
        	JarSupplier future = remoteNode.postJarStoreTransaction(new JarStoreTransactionRequest
           		(signature().getSigner(privateKey(0)), account(0),
    			ZERO, chainId, _500_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
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
        try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
        	// we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
        	// this means that the request fails and the future refers to a failed request; since this is a post,
        	// the execution does not stop, nor throws anything
        	JarSupplier future = remoteNode.postJarStoreTransaction(new JarStoreTransactionRequest
           		(signature().getSigner(privateKey(0)), account(0),
       			ZERO, chainId, _500_000, ONE, takamakaCode(), bytesOf("callernotonthis.jar"), takamakaCode()));

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
    	try (NodeService nodeRestService = NodeService.of(serviceConfig, node);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

    		TransactionReference jar = addJarStoreTransaction(privateKey(0), account(0),
    			_500_000, ONE, takamakaCode(), bytesOf("javacollections.jar"), takamakaCode());

    		var toString = (StringValue) remoteNode.runStaticMethodCallTransaction
       			(new StaticMethodCallTransactionRequest(account(0), _100_000, jar, new NonVoidMethodSignature(HASH_MAP_TESTS, "testToString1", ClassType.STRING)));

    		assertEquals("[how, are, hello, you, ?]", toString.value);
    	}
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to runInstanceMethodCallTransaction")
    void testRemoteRunInstanceMethodCallTransaction() throws Exception {
    	BigIntegerValue value;

    	try (var nodeRestService = NodeService.of(serviceConfig, node); var remoteNode = RemoteNode.of(remoteNodeconfig)) {
			var request = new InstanceMethodCallTransactionRequest
    			(account(0), _100_000, takamakaCode(), CodeSignature.NONCE, account(0));

			value = (BigIntegerValue) remoteNode.runInstanceMethodCallTransaction(request);
        }

    	assertEquals(ZERO, value.value);
    }

    private static TransactionReference getInexistentTransactionReference() {
		var reference = new JsonObject();
		// we use a non-existent hash
		reference.addProperty("hash", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
		reference.addProperty("type", "local");
	
		return new GsonBuilder().disableHtmlEscaping().create().fromJson(reference, TransactionReferenceModel.class).toBean();
	}

	private static StorageReference getInexistentStorageReference() {
		return new StorageReference(getInexistentTransactionReference(), BigInteger.valueOf(42));
	}
}