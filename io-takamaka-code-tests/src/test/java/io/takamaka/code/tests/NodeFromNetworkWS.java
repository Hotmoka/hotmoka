package io.takamaka.code.tests;

import static io.hotmoka.beans.Coin.panarea;
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
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.network.NodeService;
import io.hotmoka.network.NodeServiceConfig;
import io.hotmoka.network.RemoteNode;
import io.hotmoka.network.RemoteNodeConfig;
import io.hotmoka.network.models.values.TransactionReferenceModel;
import io.hotmoka.nodes.Node;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.verification.IncompleteClasspathError;

public class NodeFromNetworkWS extends TakamakaTest {
    private final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);
    private final ClassType HASH_MAP_TESTS = new ClassType("io.takamaka.tests.javacollections.HashMapTests");
    private final BigInteger _20_000 = BigInteger.valueOf(20_000);
    private final NodeServiceConfig serviceConfig = new NodeServiceConfig.Builder().setPort(8081).setSpringBannerModeOn(false).build();
    private final RemoteNodeConfig remoteNodeConfig = new RemoteNodeConfig.Builder()
    	.setWebSockets(true)
    	.setURL("ws://localhost:8081").build();

    @BeforeEach
    void beforeEach() throws Exception {
        setNode(ALL_FUNDS);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getTakamakaCode")
    void testRemoteGetTakamakaCode() throws Exception {
        TransactionReference localTakamakaCode = originalView.getTakamakaCode();
        TransactionReference remoteTakamakaCode;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

             remoteTakamakaCode = remoteNode.getTakamakaCode();
        }

        assertEquals(localTakamakaCode, remoteTakamakaCode);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getSignatureAlgorithmForRequests")
    void testRemoteGetSignatureAlgorithmForRequests() throws Exception {
        SignatureAlgorithm<NonInitialTransactionRequest<?>> algo;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            algo = remoteNode.getSignatureAlgorithmForRequests();
        }

        assertNotNull(algo);
        // beware below: test depending on the name of an internal class
        assertTrue(algo.getClass().getName().equals("io.hotmoka.crypto.internal.ED25519"));
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getClassTag")
    void testRemoteGetClassTag() throws Exception {
        ClassTag localClassTag = originalView.getClassTag(account(0));
        ClassTag remoteClassTag;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            remoteClassTag = remoteNode.getClassTag(account(0));
        }

        assertEquals(localClassTag, remoteClassTag);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getClassTag for a non-existing reference")
    void testRemoteGetClassTagNonExisting() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            remoteNode.getClassTag(getInexistentStorageReference());
        }
        catch (Exception e) {
            assertTrue(e instanceof NoSuchElementException);
            assertTrue(e.getMessage().equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getState")
    void testRemoteGetState() throws Exception {
        Stream<Update> localState = originalView.getState(account(0));
        Stream<Update> remoteState;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            remoteState = remoteNode.getState(account(0));
        }

        assertEquals(localState.collect(Collectors.toSet()), remoteState.collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getState for a non-existing reference")
    void testRemoteGetStateNonExisting() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            remoteNode.getState(getInexistentStorageReference());
        }
        catch (Exception e) {
            assertTrue(e instanceof NoSuchElementException);
            assertTrue(e.getMessage().equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
        }
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getRequest")
    void testRemoteGetRequest() throws Exception {
        TransactionRequest<?> request;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            request = remoteNode.getRequest(originalView.getTakamakaCode());
        }

        // the jar containing the base Takamaka code was installed by an initial jar store transaction request
        assertTrue(request instanceof JarStoreInitialTransactionRequest);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getRequest for a non-existing reference")
    void testRemoteGetRequestNonExisting() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            remoteNode.getRequest(getInexistentTransactionReference());
        }
        catch (Exception e) {
            assertTrue(e instanceof NoSuchElementException);
            assertTrue(e.getMessage().equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
            return;
        }

        fail("expected exception");
    }


    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse")
    void testRemoteGetResponse() throws Exception {
        TransactionResponse response;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            response = remoteNode.getResponse(originalView.getTakamakaCode());
        }

        // the jar containing the base Takamaka code was installed by an initial jar store transaction
        assertTrue(response instanceof JarStoreInitialTransactionResponse);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse for a non-existing reference")
    void testRemoteGetResponseNonExisting() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            remoteNode.getResponse(getInexistentTransactionReference());
        }
        catch (Exception e) {
            assertTrue(e instanceof NoSuchElementException);
            assertTrue(e.getMessage().equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
            return;
        }

        fail("expected exception");
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse for the reference of a failed request")
    void testRemoteGetResponseFailed() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
            // this means that the request fails and the future refers to a failed request; since this is a post,
            // the execution does not stop, nor throws anything
            Node.JarSupplier future = postJarStoreTransaction(privateKey(0), account(0), BigInteger.valueOf(10_000), ONE, takamakaCode(), bytesOf("lambdas.jar")
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
            assertTrue(e.getMessage().contains(IncompleteClasspathError.class.getName()));
            return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getPolledResponse")
    void testRemoteGetPolledResponse() throws Exception {
        TransactionResponse response;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            // we install a jar in blockchain
            Node.JarSupplier future = postJarStoreTransaction(privateKey(0), account(0), BigInteger.valueOf(10_000), ONE,
                    takamakaCode(), bytesOf("lambdas.jar"), takamakaCode());

            // we poll for its result
            response = remoteNode.getPolledResponse(future.getReferenceOfRequest());
        }

        // lambdas.jar has been correctly installed in the node, hence the response is successful
        assertTrue(response instanceof JarStoreTransactionSuccessfulResponse);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getPolledResponse for a non-existing reference")
    void testRemoteGetPolledResponseNonExisting() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

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
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
            // this means that the request fails and the future refers to a failed request; since this is a post,
            // the execution does not stop, nor throws anything
            Node.JarSupplier future = postJarStoreTransaction(privateKey(0), account(0), BigInteger.valueOf(10_000), ONE, takamakaCode(), bytesOf("lambdas.jar")
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
            remoteNode.getPolledResponse(future.getReferenceOfRequest());
        }
        catch (TransactionRejectedException e) {
            assertTrue(e.getMessage().contains(IncompleteClasspathError.class.getName()));
            return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network service from a Hotmoka node and makes a remote call to addJarStoreTransaction")
    void testRemoteAddJarStoreTransaction() throws Exception {
        TransactionReference transaction;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            transaction = remoteNode.addJarStoreTransaction(new JarStoreTransactionRequest
                    (NonInitialTransactionRequest.Signer.with(signature(), privateKey(0)), account(0),
                            ZERO, chainId, _20_000, ONE, takamakaCode(), bytesOf("lambdas.jar"), takamakaCode()));
        }

        assertNotNull(transaction);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to addJarStoreTransactionRequest for a request that gets rejected")
    void testRemoteAddJarStoreTransactionRejected() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
            // this means that the request fails and the future refers to a failed request; since this is a post,
            // the execution does not stop, nor throws anything
            remoteNode.addJarStoreTransaction(new JarStoreTransactionRequest
                    (NonInitialTransactionRequest.Signer.with(signature(), privateKey(0)), account(0),
                            ZERO, chainId, _20_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
                            // , takamakaCode() // <-- forgot that
                    ));
        }
        catch (TransactionRejectedException e) {
            assertTrue(e.getMessage().contains(IncompleteClasspathError.class.getName()));
            return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to addJarStoreTransactionRequest for a request that fails")
    void testRemoteAddJarStoreTransactionFailed() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            remoteNode.addJarStoreTransaction(new JarStoreTransactionRequest
                    (NonInitialTransactionRequest.Signer.with(signature(), privateKey(0)), account(0),
                            ZERO, chainId, _20_000, ONE, takamakaCode(), bytesOf("callernotonthis.jar"), takamakaCode()));
        }
        catch (TransactionException e) {
            assertTrue(e.getMessage().contains("io.takamaka.code.verification.VerificationException"));
            assertTrue(e.getMessage().contains("caller() can only be called on \"this\""));
            return;
        }

        fail("expected exception");
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to postJarStoreTransactionRequest")
    void testRemotePostJarStoreTransaction() throws Exception {
        TransactionReference transaction;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            Node.JarSupplier future = remoteNode.postJarStoreTransaction(new JarStoreTransactionRequest
                    (NonInitialTransactionRequest.Signer.with(signature(), privateKey(0)), account(0),
                            ZERO, chainId, _20_000, ONE, takamakaCode(), bytesOf("lambdas.jar"), takamakaCode()));

            // we wait until the request has been processed
            transaction = future.get();
        }

        assertNotNull(transaction);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to postJarStoreTransactionRequest for a request that gets rejected")
    void testRemotePostJarStoreTransactionRejected() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
            // this means that the request fails and the future refers to a failed request; since this is a post,
            // the execution does not stop, nor throws anything
            Node.JarSupplier future = remoteNode.postJarStoreTransaction(new JarStoreTransactionRequest
                    (NonInitialTransactionRequest.Signer.with(signature(), privateKey(0)), account(0),
                            ZERO, chainId, _20_000, ONE, takamakaCode(), bytesOf("lambdas.jar")
                            // , takamakaCode() // <-- forgot that
                    ));

            // we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
            // since the request failed and its transaction was rejected
            future.get();
        }
        catch (TransactionRejectedException e) {
            assertTrue(e.getMessage().contains(IncompleteClasspathError.class.getName()));
            return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to postJarStoreTransactionRequest for a request that fails")
    void testRemotePostJarStoreTransactionFailed() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
            // this means that the request fails and the future refers to a failed request; since this is a post,
            // the execution does not stop, nor throws anything
            Node.JarSupplier future = remoteNode.postJarStoreTransaction(new JarStoreTransactionRequest
                    (NonInitialTransactionRequest.Signer.with(signature(), privateKey(0)), account(0),
                            ZERO, chainId, _20_000, ONE, takamakaCode(), bytesOf("callernotonthis.jar"), takamakaCode()));

            // we wait until the request has been processed; this will throw a TransactionException at the end,
            // since the request was accepted but its execution failed
            future.get();
        }
        catch (TransactionException e) {
            assertTrue(e.getMessage().contains("io.takamaka.code.verification.VerificationException"));
            assertTrue(e.getMessage().contains("caller() can only be called on \"this\""));
            return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to runStaticMethodCallTransaction")
    void testRemoteRunStaticMethodCallTransaction() throws Exception {
        StringValue toString;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            TransactionReference jar = addJarStoreTransaction(privateKey(0), account(0),
                    _20_000, ONE, takamakaCode(), bytesOf("javacollections.jar"), takamakaCode());

            toString = (StringValue) remoteNode.runStaticMethodCallTransaction
                    (new StaticMethodCallTransactionRequest(NonInitialTransactionRequest.Signer.with(signature(), privateKey(0)), account(0), ZERO, chainId,
                            _20_000, panarea(1), jar, new NonVoidMethodSignature(HASH_MAP_TESTS, "testToString1", ClassType.STRING)));
        }

        assertEquals("[how, are, hello, you, ?]", toString.value);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to runInstanceMethodCallTransaction")
    void testRemoteRunInstanceMethodCallTransaction() throws Exception {
        BigIntegerValue value;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
             RemoteNode remoteNode = RemoteNode.of(remoteNodeConfig)) {

            NonInitialTransactionRequest.Signer signer = NonInitialTransactionRequest.Signer.with(originalView.getSignatureAlgorithmForRequests(), privateKey(0));
            InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest
                    (signer, account(0), ZERO, chainId, _20_000, panarea(1), takamakaCode(),
                            new NonVoidMethodSignature(Constants.ACCOUNT_NAME, "nonce", ClassType.BIG_INTEGER), account(0));

            value = (BigIntegerValue) remoteNode.runInstanceMethodCallTransaction(request);
        }

        assertEquals(ZERO, value.value);
    }

    private static TransactionReference getInexistentTransactionReference() {
        JsonObject reference = new JsonObject();
        // we use a non-existent hash
        reference.addProperty("hash", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        reference.addProperty("type", "local");

        return new GsonBuilder().disableHtmlEscaping().create().fromJson(reference, TransactionReferenceModel.class).toBean();
    }

    private static StorageReference getInexistentStorageReference() {
        return new StorageReference(getInexistentTransactionReference(), BigInteger.valueOf(42));
    }
}
