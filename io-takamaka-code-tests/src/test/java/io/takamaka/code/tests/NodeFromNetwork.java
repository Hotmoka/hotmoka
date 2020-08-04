package io.takamaka.code.tests;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest.Signer;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.network.NodeService;
import io.hotmoka.network.NodeServiceConfig;
import io.hotmoka.network.RemoteNode;
import io.hotmoka.network.RemoteNodeConfig;
import io.hotmoka.network.models.values.StorageReferenceModel;

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

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView)) {
            try (RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {
            	remoteTakamakaCode = remoteNode.getTakamakaCode();
            }
        }

        assertEquals(localTakamakaCode, remoteTakamakaCode);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getSignatureAlgorithmForRequests")
    void testRemoteSignatureAlgorithmForRequests() throws Exception {
    	SignatureAlgorithm<NonInitialTransactionRequest<?>> algo = null;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView)) {
            try (RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {
            	algo = remoteNode.getSignatureAlgorithmForRequests();
            }
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

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView)) {
            try (RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {
                remoteClassTag = remoteNode.getClassTag(account(0));
            }
        }

        assertEquals(localClassTag, remoteClassTag);
    }

    @Test
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getClassTag ending with a NoSuchElementException for a non existing reference")
    void testRemoteClassTagNoSuchElement() throws Exception {
        Exception e = null;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView)) {
            try (RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {
                JsonObject reference = new JsonObject();
                // we use a non-existent hash
                reference.addProperty("hash", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
                reference.addProperty("type", "local");

                JsonObject object = new JsonObject();
                object.addProperty("progressive", 0);
                object.add("transaction", reference);

                Gson gson = new Gson();
                StorageReferenceModel inexistentStorageReferenceModel = gson.fromJson(object.toString(), StorageReferenceModel.class);

                try {
                    remoteNode.getClassTag(inexistentStorageReferenceModel.toBean());
                }
                catch (Exception ee) {
                	e = ee;
                }
            }
        }

        assertTrue(e instanceof NoSuchElementException);
        assertTrue(e.getMessage().equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
    }

    @Test
    @Disabled
    @DisplayName("starts a network server from a Hotmoka node and makes a remote call to getResponse for an existing reference")
    void testRemoteGetResponse() throws Exception {
    	TransactionResponse response = null;

    	try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView)) {
            try (RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {
            	response = remoteNode.getResponseAt(originalView.getTakamakaCode());
            }
        }

    	System.out.println(response);
    }

    @Test
    @DisplayName("starts a network service from a Hotmoka node and tries to install an illegal jar")
    void testRemoteInstallationOfIllegalJar() throws Exception {
        Exception e = null;

        try (NodeService nodeRestService = NodeService.of(serviceConfig, originalView)) {
            try (RemoteNode remoteNode = RemoteNode.of(remoteNodeconfig)) {

                try {
                    remoteNode.addJarStoreTransaction(new JarStoreTransactionRequest
                            (Signer.with(signature(), privateKey(0)), account(0),
                                    ZERO, chainId, _20_000, ONE, takamakaCode(), bytesOf("callernotonthis.jar"), takamakaCode()));
                }
                catch (Exception ee) {
                    e = ee;
                }
            }
        }

        assertTrue(e instanceof TransactionException);
        assertTrue(e.getMessage().contains("io.takamaka.code.verification.VerificationException"));
        assertTrue(e.getMessage().contains("caller() can only be called on \"this\""));
    }
}