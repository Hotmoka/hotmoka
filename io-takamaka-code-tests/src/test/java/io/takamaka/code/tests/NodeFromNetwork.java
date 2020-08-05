package io.takamaka.code.tests;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest.Signer;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.network.NodeService;
import io.hotmoka.network.NodeServiceConfig;
import io.hotmoka.network.RemoteNode;
import io.hotmoka.network.RemoteNodeConfig;
import io.hotmoka.network.models.values.TransactionReferenceModel;
import io.hotmoka.nodes.Node.JarSupplier;
import io.takamaka.code.verification.IncompleteClasspathError;

public class NodeFromNetwork extends TakamakaTest {
    private final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);
    private final BigInteger _20_000 = BigInteger.valueOf(20_000);
    private final NodeServiceConfig serviceConfig = new NodeServiceConfig.Builder().setPort(8080).setSpringBannerModeOn(false).build();
    private final RemoteNodeConfig remoteNodeconfig = new RemoteNodeConfig.Builder().setURL("http://localhost:8080").build();

    @BeforeEach
    void beforeEach() throws Exception {
        setNode(ALL_FUNDS);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getTakamakaCode")
    void testRemoteTakamakaCode() throws Exception {
    	TransactionReference localTakamakaCode = originalView.getTakamakaCode();
    	TransactionReference remoteTakamakaCode;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

        	remoteTakamakaCode = remoteNode.getTakamakaCode();
        }

        assertEquals(localTakamakaCode, remoteTakamakaCode);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getSignatureAlgorithmForRequests")
    void testRemoteSignatureAlgorithmForRequests() throws Exception {
    	SignatureAlgorithm<NonInitialTransactionRequest<?>> algo = null;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

        	algo = remoteNode.getSignatureAlgorithmForRequests();
        }

        assertNotNull(algo);
        // beware below: test depending on the name of an internal class
        assertTrue(algo.getClass().getName().equals("io.hotmoka.crypto.internal.SHA256DSA"));
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getClassTag")
    void testRemoteClassTag() throws Exception {
    	ClassTag localClassTag = originalView.getClassTag(account(0));
        ClassTag remoteClassTag;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

        	remoteClassTag = remoteNode.getClassTag(account(0));
        }

        assertEquals(localClassTag, remoteClassTag);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getClassTag ending with a NoSuchElementException for a non existing reference")
    void testRemoteClassTagNoSuchElement() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

        	remoteNode.getClassTag(getInexistentStorageReference());
        }
        catch (Exception e) {
        	assertTrue(e instanceof NoSuchElementException);
        	assertTrue(e.getMessage().equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
        }
    }

	@Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse for an existing reference")
    void testRemoteGetResponse() throws Exception {
    	TransactionResponse response = null;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

        	response = remoteNode.getResponseAt(originalView.getTakamakaCode());
        }

        // the jar containing the base Takamaka code was installed by an initial
        // transaction that terminated successfully
    	assertTrue(response instanceof JarStoreInitialTransactionResponse);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse for a non-existing reference")
    void testRemoteGetResponseNonExisting() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

        	remoteNode.getResponseAt(getInexistentTransactionReference());
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
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

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
        	remoteNode.getResponseAt(future.getReferenceOfRequest());
        }
        catch (TransactionRejectedException e) {
        	assertTrue(e.getMessage().contains(IncompleteClasspathError.class.getName()));
        	return;
        }

        fail();
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getPolledResponse for an existing reference")
    void testRemoteGetPolledResponse() throws Exception {
    	TransactionResponse response = null;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

        	// we install a jar in blockchain
        	JarSupplier future = postJarStoreTransaction(privateKey(0), account(0), BigInteger.valueOf(10_000), ONE,
        		takamakaCode(), bytesOf("lambdas.jar"), takamakaCode());

        	// we poll for its result
        	response = remoteNode.getPolledResponseAt(future.getReferenceOfRequest());
        }

        // lambdas.jar has been correctly installed in the node, hence the response is successful
    	assertTrue(response instanceof JarStoreTransactionSuccessfulResponse);
    }

    @Test
    @DisplayName("starts a network service from a Hotmoka node and tries to install an illegal jar")
    void testRemoteInstallationOfIllegalJar() throws Exception {
        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView);
        	 RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

            remoteNode.addJarStoreTransaction(new JarStoreTransactionRequest
       			(Signer.with(signature(), privateKey(0)), account(0),
				ZERO, chainId, _20_000, ONE, takamakaCode(), bytesOf("callernotonthis.jar"), takamakaCode()));
        }
        catch (Exception e) {
        	assertTrue(e instanceof TransactionException);
        	assertTrue(e.getMessage().contains("io.takamaka.code.verification.VerificationException"));
        	assertTrue(e.getMessage().contains("caller() can only be called on \"this\""));
        	return;
        }

        fail("expected exception");
    }

	private static TransactionReference getInexistentTransactionReference() {
		JsonObject reference = new JsonObject();
		// we use a non-existent hash
		reference.addProperty("hash", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
		reference.addProperty("type", "local");
	
		return new Gson().fromJson(reference, TransactionReferenceModel.class).toBean();
	}

	private static StorageReference getInexistentStorageReference() {
		return new StorageReference(getInexistentTransactionReference(), BigInteger.valueOf(42));
	}
}