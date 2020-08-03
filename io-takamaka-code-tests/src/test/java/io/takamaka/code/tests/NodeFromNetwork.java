package io.takamaka.code.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.network.NodeService;
import io.hotmoka.network.NodeServiceConfig;
import io.hotmoka.network.RemoteNode;
import io.hotmoka.network.RemoteNodeConfig;
import io.hotmoka.network.models.values.StorageReferenceModel;

public class NodeFromNetwork extends TakamakaTest {
    private final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);
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
        String exceptionType = null;

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
                catch (NoSuchElementException e) {
                    exceptionType = e.getClass().getName();
                }
            }
        }

        assertEquals(NoSuchElementException.class.getName(), exceptionType);
    }
}