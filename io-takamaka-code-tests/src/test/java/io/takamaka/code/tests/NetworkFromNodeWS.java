/**
 *
 */
package io.takamaka.code.tests;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.NodeService;
import io.hotmoka.network.NodeServiceConfig;
import io.hotmoka.network.internal.websocket.WebsocketClient;
import io.hotmoka.network.models.errors.ErrorModel;
import io.hotmoka.network.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.models.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static io.hotmoka.beans.types.BasicTypes.INT;
import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A test for creating a network server from a Hotmoka node using websockets
 */
class NetworkFromNodeWS extends TakamakaTest {
    private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);
    private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
    private static final ConstructorSignature CONSTRUCTOR_INTERNATIONAL_TIME = new ConstructorSignature("io.takamaka.tests.basicdependency.InternationalTime", INT, INT, INT);

    private final NodeServiceConfig configNoBanner = new NodeServiceConfig.Builder().setPort(8081).setSpringBannerModeOn(false).build();

    /**
     * The account that holds all funds.
     */
    private StorageReference master;

    /**
     * The classpath of the classes being tested.
     */
    private TransactionReference classpath;

    /**
     * The private key of {@linkplain #master}.
     */
    private PrivateKey key;

    @BeforeEach
    void beforeEach() throws Exception {
        setNode("basicdependency.jar", ALL_FUNDS, BigInteger.ZERO);
        master = account(0);
        key = privateKey(0);
        classpath = addJarStoreTransaction(key, master, BigInteger.valueOf(10000), BigInteger.ONE, takamakaCode(), bytesOf("basic.jar"), jar());
    }

    @Test @DisplayName("starts a network server from a Hotmoka node")
    void startNetworkFromNode() {
        NodeServiceConfig config = new NodeServiceConfig.Builder().setPort(8081).setSpringBannerModeOn(true).build();
        try (NodeService nodeRestService = NodeService.of(config, nodeWithJarsView)) {
        }
    }


    @Test @DisplayName("starts a network server from a Hotmoka node and checks its signature algorithm")
    void startNetworkFromNodeAndTestSignatureAlgorithm() throws ExecutionException, InterruptedException {

        try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView);
             WebsocketClient wsClient = new WebsocketClient("ws://localhost:8081/node")) {

            final AtomicInteger semaphore = new AtomicInteger();

            wsClient.subscribe("/user/" + wsClient.getClientKey() + "/get/errors", new WebsocketClient.SubscriptionResponseHandler<>(ErrorModel.class, response -> {
                synchronized (semaphore) {
                    semaphore.notify();
                }
            }));

            wsClient.subscribe("/topic/get/signatureAlgorithmForRequests", new WebsocketClient.SubscriptionResponseHandler<>(SignatureAlgorithmResponseModel.class, response -> {
                assertEquals("ed25519", response.algorithm);
                synchronized (semaphore) {
                    semaphore.incrementAndGet();
                    semaphore.notify();
                }
            }));

            wsClient.send("/get/signatureAlgorithmForRequests");

            synchronized (semaphore) {
                semaphore.wait();
            }

            if (semaphore.get() == 0) {
                fail("failed");
            }
        }

    }

    @Test @DisplayName("starts a network server from a Hotmoka node and runs getTakamakaCode()")
    void testGetTakamakaCode() throws ExecutionException, InterruptedException {

        try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView);
             WebsocketClient wsClient = new WebsocketClient("ws://localhost:8081/node")) {

            final AtomicInteger semaphore = new AtomicInteger();

            wsClient.subscribe("/user/" + wsClient.getClientKey() + "/get/errors", new WebsocketClient.SubscriptionResponseHandler<>(ErrorModel.class, response -> {
                synchronized (semaphore) {
                    semaphore.notify();
                }
            }));

            wsClient.subscribe("/topic/get/takamakaCode", new WebsocketClient.SubscriptionResponseHandler<>(TransactionReferenceModel.class, response -> {
                assertEquals(nodeWithJarsView.getTakamakaCode().getHash(), response.hash);
                synchronized (semaphore) {
                    semaphore.incrementAndGet();
                    semaphore.notify();
                }
            }));

            wsClient.send("/get/takamakaCode");

            synchronized (semaphore) {
                semaphore.wait();
            }

            if (semaphore.get() == 0) {
                fail("failed");
            }
        }

    }


    @Test @DisplayName("starts a network server from a Hotmoka node and runs addJarStoreInitialTransaction()")
    void addJarStoreInitialTransaction() throws InterruptedException, IOException, ExecutionException {

        try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView);
             WebsocketClient wsClient = new WebsocketClient("ws://localhost:8081/node")) {
            JarStoreInitialTransactionRequest request = new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("jars/c13.jar")), nodeWithJarsView.getTakamakaCode());

            final AtomicInteger semaphore = new AtomicInteger();

            wsClient.subscribe("/user/" + wsClient.getClientKey() + "/add/errors", new WebsocketClient.SubscriptionResponseHandler<>(ErrorModel.class, response -> {
                assertNotNull(response);
                assertEquals("cannot run a JarStoreInitialTransactionRequest in an already initialized node", response.message);
                assertEquals(TransactionRejectedException.class.getName(), response.exceptionClassName);

                synchronized (semaphore) {
                    semaphore.incrementAndGet();
                    semaphore.notify();
                }
            }));

            wsClient.subscribe("/topic/add/jarStoreInitialTransaction", new WebsocketClient.SubscriptionResponseHandler<>(StorageReferenceModel.class, response -> {
                synchronized (semaphore) {
                    semaphore.notify();
                }
            }));

            wsClient.send("/add/jarStoreInitialTransaction", new JarStoreInitialTransactionRequestModel(request));

            synchronized (semaphore) {
                semaphore.wait();
            }

            if (semaphore.get() == 0) {
                fail("failed");
            }
        }
    }

    @Test @DisplayName("starts a network server from a Hotmoka node and runs addJarStoreInitialTransaction() without a jar")
    void addJarStoreInitialTransactionWithoutJar() throws InterruptedException, ExecutionException {

        try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView);
             WebsocketClient wsClient = new WebsocketClient("ws://localhost:8081/node")) {

            final AtomicInteger semaphore = new AtomicInteger();

            wsClient.subscribe("/user/" + wsClient.getClientKey() + "/add/errors", new WebsocketClient.SubscriptionResponseHandler<>(ErrorModel.class, response -> {
                assertNotNull(response);
                assertEquals("unexpected null jar", response.message);
                assertEquals(InternalFailureException.class.getName(), response.exceptionClassName);

                synchronized (semaphore) {
                    semaphore.incrementAndGet();
                    semaphore.notify();
                }
            }));

            wsClient.subscribe("/topic/add/jarStoreInitialTransaction", new WebsocketClient.SubscriptionResponseHandler<>(StorageReferenceModel.class, response -> {
                synchronized (semaphore) {
                    semaphore.notify();
                }
            }));

            wsClient.send("/add/jarStoreInitialTransaction", new JarStoreInitialTransactionRequestModel());

            synchronized (semaphore) {
                semaphore.wait();
            }

            if (semaphore.get() == 0) {
                fail("failed");
            }

        }
    }

    @Test @DisplayName("starts a network server from a Hotmoka node and calls addConstructorCallTransaction - new Sub(1973)")
    void addConstructorCallTransaction() throws InterruptedException, SignatureException, InvalidKeyException, NoSuchAlgorithmException, ExecutionException {

        try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView);
             WebsocketClient wsClient = new WebsocketClient("ws://localhost:8081/node")) {

            ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest(
                    NonInitialTransactionRequest.Signer.with(signature(), key),
                    master,
                    ONE,
                    chainId,
                    _20_000,
                    ONE,
                    classpath,
                    new ConstructorSignature("io.takamaka.tests.basic.Sub", INT),
                    new IntValue(1973)
            );

            final AtomicInteger semaphore = new AtomicInteger();

            wsClient.subscribe("/user/" + wsClient.getClientKey() + "/add/errors", new WebsocketClient.SubscriptionResponseHandler<>(ErrorModel.class, response -> {
                synchronized (semaphore) {
                    semaphore.notify();
                }
            }));

            wsClient.subscribe("/topic/add/constructorCallTransaction", new WebsocketClient.SubscriptionResponseHandler<>(StorageReferenceModel.class, response -> {
                assertNotNull(response.transaction);
                synchronized (semaphore) {
                    semaphore.incrementAndGet();
                    semaphore.notify();
                }
            }));

            wsClient.send("/add/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request));

            synchronized (semaphore) {
                semaphore.wait();
            }

            if (semaphore.get() == 0) {
                fail("failed");
            }
        }
    }

    @Test @DisplayName("starts a network server from a Hotmoka node, creates an object and calls getState() on it")
    void testGetState() throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, ExecutionException, InterruptedException {

        try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView);
             WebsocketClient wsClient = new WebsocketClient("ws://localhost:8081/node")) {

            ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest(
                    NonInitialTransactionRequest.Signer.with(signature(), key),
                    master,
                    ONE,
                    chainId,
                    _20_000,
                    ONE,
                    classpath,
                    CONSTRUCTOR_INTERNATIONAL_TIME,
                    new IntValue(13), new IntValue(25), new IntValue(40)
            );

            final AtomicInteger semaphore = new AtomicInteger();

            wsClient.subscribe("/user/" + wsClient.getClientKey() + "/add/errors", new WebsocketClient.SubscriptionResponseHandler<>(ErrorModel.class, response -> {
                synchronized (semaphore) {
                    semaphore.notify();
                }
            }));

            wsClient.subscribe("/user/" + wsClient.getClientKey() + "/get/errors", new WebsocketClient.SubscriptionResponseHandler<>(ErrorModel.class, response -> {
                synchronized (semaphore) {
                    semaphore.notify();
                }
            }));


            wsClient.subscribe("/topic/add/constructorCallTransaction", new WebsocketClient.SubscriptionResponseHandler<>(StorageReferenceModel.class, response -> {
                assertNotNull(response.transaction);

                // we query the state of the object
                wsClient.send("/get/state", response);
            }));

            wsClient.subscribe("/topic/get/state", new WebsocketClient.SubscriptionResponseHandler<>(StateModel.class, response -> {
                // the state contains two updates
                assertSame(2, response.updates.size());
                synchronized (semaphore) {
                    semaphore.incrementAndGet();
                    semaphore.notify();
                }
            }));

            // we execute the creation of the object
            wsClient.send("/add/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request));

            synchronized (semaphore) {
                semaphore.wait();
            }

            if (semaphore.get() == 0) {
                fail("failed");
            }
        }
    }

    @Test @DisplayName("starts a network server from a Hotmoka node, creates an object and calls getState() on it")
    void testGetClassTag() throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, ExecutionException, InterruptedException {

        try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView);
             WebsocketClient wsClient = new WebsocketClient("ws://localhost:8081/node")) {

            ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest(
                    NonInitialTransactionRequest.Signer.with(signature(), key),
                    master,
                    ONE,
                    chainId,
                    _20_000,
                    ONE,
                    classpath,
                    CONSTRUCTOR_INTERNATIONAL_TIME,
                    new IntValue(13), new IntValue(25), new IntValue(40)
            );

            final AtomicInteger semaphore = new AtomicInteger();

            wsClient.subscribe("/user/" + wsClient.getClientKey() +"/add/errors", new WebsocketClient.SubscriptionResponseHandler<>(ErrorModel.class, response -> {
                synchronized (semaphore) {
                    semaphore.notify();
                }
            }));

            wsClient.subscribe("/user/" + wsClient.getClientKey() +"/get/errors", new WebsocketClient.SubscriptionResponseHandler<>(ErrorModel.class, response -> {
                synchronized (semaphore) {
                    semaphore.notify();
                }
            }));

            wsClient.subscribe("/topic/add/constructorCallTransaction", new WebsocketClient.SubscriptionResponseHandler<>(StorageReferenceModel.class, response -> {
                assertNotNull(response.transaction);

                // we query the class tag of the object
                wsClient.send("/get/classTag", response);
            }));

            wsClient.subscribe("/topic/get/classTag", new WebsocketClient.SubscriptionResponseHandler<>(ClassTagModel.class, response -> {
                
                // the state that the class tag holds the name of the class that has been created
                assertEquals(CONSTRUCTOR_INTERNATIONAL_TIME.definingClass.name, response.className);
                synchronized (semaphore) {
                    semaphore.incrementAndGet();
                    semaphore.notify();
                }
            }));

            // we execute the creation of the object
            wsClient.send("/add/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request));

            synchronized (semaphore) {
                semaphore.wait();
            }

            if (semaphore.get() == 0) {
                fail("failed");
            }
        }

    }
}