import io.hotmoka.network.thin.client.RemoteNodeClient
import io.hotmoka.network.thin.client.exceptions.TransactionException
import io.hotmoka.network.thin.client.exceptions.TransactionRejectedException
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.JarStoreInitialTransactionResponseModel
import io.hotmoka.network.thin.client.models.responses.JarStoreTransactionSuccessfulResponseModel
import io.hotmoka.network.thin.client.models.responses.TransactionRestResponseModel
import io.hotmoka.network.thin.client.models.signatures.ConstructorSignatureModel
import io.hotmoka.network.thin.client.models.signatures.MethodSignatureModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
import io.hotmoka.network.thin.client.suppliers.JarSupplier
import io.hotmoka.network.thin.client.webSockets.StompClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UninitializedRemoteNodeTest {
    /**
     * The url of the remote node
     */
    private val url = "localhost:8080"

    /**
     * The takamaka jar version
     */
    private val takamakaJarVersion = "1.0.0"

    /**
     * The chain if of the node
     */
    private val chainId = "io.takamaka.code.tests.TakamakaTest"

    /**
     * The takamaka code blockchain reference
     */
    private lateinit var takamakaCode: TransactionReferenceModel

    /**
     * The gamete blockchain reference
     */
    private lateinit var gamete: StorageReferenceModel

    /**
     * The manifest blockchain reference
     */
    private lateinit var manifest: StorageReferenceModel


    /**
     * Data for tests
     */
    private val nonExistingTransactionReference = TransactionReferenceModel(
        "local",
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    )
    private val nonExistingStorageReference = StorageReferenceModel(nonExistingTransactionReference, "2")
    private val eventModel = EventRequestModel(
        StorageReferenceModel(
            TransactionReferenceModel("local", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
            "0"
        ),
        StorageReferenceModel(
            TransactionReferenceModel("local", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
            "0"
        )
    )

    /**
     * It initializes the hotmoka remote node
     */
    init {
        initializeRemoteNode()
    }


    @Test
    fun getTakamakaCode() {
        RemoteNodeClient(url).use { client ->

            val takamakaCode = client.getTakamakaCode()

            assertNotNull(takamakaCode, "expected takamakaCode to be not null")
            assertEquals(this.takamakaCode.hash, takamakaCode.hash)
            assertEquals(this.takamakaCode.type, takamakaCode.type)
            assertEquals("local", this.takamakaCode.type)
        }
    }

    @Test
    fun getSignatureAlgorithmForRequests() {
        RemoteNodeClient(url).use { client ->

            val algorithm = client.getSignatureAlgorithmForRequests()

            assertNotNull(algorithm)
            assertEquals("empty", algorithm.algorithm)
        }
    }

    @Test
    fun getManifest() {
        RemoteNodeClient(url).use { client ->

            val reference = client.getManifest()

            assertNotNull(reference, "expected result to be not null")
            assertNotNull(reference.transaction, "expected transaction to be not null")
            assertEquals(this.manifest.transaction.hash, reference.transaction.hash)
            assertEquals(this.manifest.transaction.type, reference.transaction.type)
            assertEquals("local", reference.transaction.type)
        }
    }

    @Test
    fun getState() {
        RemoteNodeClient(url).use { client ->

            val manifestReference = client.getManifest()
            val state = client.getState(manifestReference)

            assertNotNull(state, "expected state to be not null")
            assertEquals(2, state.updates.size)
            assertNotNull(state.updates[0].updatedObject, "expected updateObject to not null")
            assertEquals(this.manifest.transaction.hash, state.updates[0].updatedObject.transaction.hash)
        }
    }

    @Test
    fun getStateNonExisting() {
        RemoteNodeClient(url).use { client ->

            try {
                client.getState(nonExistingStorageReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun getClassTag() {
        RemoteNodeClient(url).use { client ->

            val manifestReference = client.getManifest()
            val classTag = client.getClassTag(manifestReference)

            assertNotNull(classTag, "expected classTag to be not null")
            assertEquals("io.takamaka.code.system.Manifest", classTag.className)
            assertNotNull(classTag.jar.hash, "expected classTag jar to be not null")
            assertEquals(this.takamakaCode.hash, classTag.jar.hash)
        }
    }

    @Test
    fun getClassTagNonExisting() {
        RemoteNodeClient(url).use { client ->

            try {
                client.getClassTag(nonExistingStorageReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun getRequest() {
        RemoteNodeClient(url).use { client ->

            val transactionRequest = client.getRequest(this.takamakaCode)

            assertNotNull(transactionRequest, "expected transactionRequest to be not null")
            assertTrue(
                transactionRequest.transactionResponseModel is JarStoreInitialTransactionRequestModel,
                "expected transaction request model to be of type JarStoreInitialTransactionResponseModel"
            )
        }
    }

    @Test
    fun getRequestNonExisting() {
        RemoteNodeClient(url).use { client ->

            try {
                client.getRequest(nonExistingTransactionReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun getResponse() {
        RemoteNodeClient(url).use { client ->

            val transactionResponse = client.getResponse(this.takamakaCode)

            assertNotNull(transactionResponse, "expected transactionResponse to be not null")
            assertTrue(
                transactionResponse.transactionResponseModel is JarStoreInitialTransactionResponseModel,
                "expected transaction response model to be of type JarStoreInitialTransactionResponseModel"
            )
        }
    }

    @Test
    fun getResponseNonExisting() {
        RemoteNodeClient(url).use { client ->

            try {
                client.getRequest(nonExistingTransactionReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun getResponseFailed() {
        RemoteNodeClient(url).use { client ->

            try {

                // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
                // this means that the request fails and the future refers to a failed request; since this is a post,
                // the execution does not stop, nor throws anything
                val jarSupplier: JarSupplier
                try {
                    jarSupplier = client.postJarStoreTransaction(
                        JarStoreTransactionRequestModel(
                            "",
                            this.gamete,
                            getGameteNonce(),
                            this.takamakaCode,
                            this.chainId,
                            "20000",
                            "1",
                            getJarExampleOf("lambdas"),
                            listOf()
                        )
                    )

                } catch (e: Exception) {
                    fail("unexpected exception")
                }

                // we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
                // since the request failed and its transaction was rejected
                try {
                    jarSupplier.get()
                } catch (e: Exception) {

                }

                // if we ask for the outcome of the request, we will get the TransactionRejectedException as answer
                client.getResponse(jarSupplier.getReferenceOfRequest())

            } catch (e: TransactionRejectedException) {
                assertTrue(e.message!!.contains("io.takamaka.code.verification.IncompleteClasspathError"))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun getPolledResponse() {
        val transactionResponse: TransactionRestResponseModel<*>

        RemoteNodeClient(url).use { client ->

            val jarSupplier = client.postJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    getGameteNonce(),
                    this.takamakaCode,
                    this.chainId,
                    "20000",
                    "1",
                    getJarExampleOf("lambdas"),
                    listOf(this.takamakaCode)
                )
            )

            transactionResponse = client.getPolledResponse(jarSupplier.getReferenceOfRequest())
        }

        assertNotNull(transactionResponse)
        assertNotNull(transactionResponse.transactionResponseModel)
        assertTrue(transactionResponse.transactionResponseModel is JarStoreTransactionSuccessfulResponseModel)
    }

    @Test
    fun getPolledResponseNonExisting() {
        RemoteNodeClient(url).use { client ->

            try {
                client.getPolledResponse(nonExistingTransactionReference)
            } catch (e: Exception) {
                assertTrue(e is TimeoutException, "expected exception to of type TimeoutException")
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun getPolledResponseFailed() {
        RemoteNodeClient(url).use { client ->

            try {

                // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
                // this means that the request fails and the future refers to a failed request; since this is a post,
                // the execution does not stop, nor throws anything
                val jarSupplier: JarSupplier
                try {
                    jarSupplier = client.postJarStoreTransaction(
                        JarStoreTransactionRequestModel(
                            "",
                            this.gamete,
                            getGameteNonce(),
                            this.takamakaCode,
                            this.chainId,
                            "20000",
                            "1",
                            getJarExampleOf("lambdas"),
                            listOf()
                        )
                    )

                } catch (e: Exception) {
                    fail("unexpected exception")
                }

                // we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
                // since the request failed and its transaction was rejected
                try {
                    jarSupplier.get()
                } catch (e: Exception) {

                }

                // if we ask for the outcome of the request, we will get the TransactionRejectedException as answer
                client.getPolledResponse(jarSupplier.getReferenceOfRequest())

            } catch (e: TransactionRejectedException) {
                assertTrue(e.message!!.contains("io.takamaka.code.verification.IncompleteClasspathError"))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun addJarStoreInitialTransaction() {
        RemoteNodeClient(url).use { client ->

            try {
                client.addJarStoreInitialTransaction(
                    JarStoreInitialTransactionRequestModel(
                        getJarTestOf("c13"),
                        listOf(client.getTakamakaCode())
                    )
                )

            } catch (e: Exception) {
                assertTrue(
                    e is TransactionRejectedException,
                    "expected exception to of type TransactionRejectedException"
                )
                assertTrue(e.message!!.equals("cannot run a JarStoreInitialTransactionRequest in an already initialized node"))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun addJarStoreTransaction() {
        RemoteNodeClient(url).use { client ->

            val takamakaCode = client.getTakamakaCode()
            val transaction = client.addJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    getGameteNonce(),
                    takamakaCode,
                    this.chainId,
                    "20000",
                    "1",
                    getJarTestOf("c13"),
                    listOf(takamakaCode)
                )
            )

            assertNotNull(transaction)
        }
    }

    @Test
    fun addJarStoreTransactionRejected() {
        RemoteNodeClient(url).use { client ->

            val incorrectClasspath = TransactionReferenceModel("local", "")

            try {
                client.addJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        getGameteNonce(),
                        incorrectClasspath,
                        this.chainId,
                        "20000",
                        "1",
                        getJarTestOf("c13"),
                        listOf()
                    )
                )
            } catch (e: Exception) {
                assertTrue(
                    e is TransactionRejectedException,
                    "expected exception to of type TransactionRejectedException"
                )
                assertTrue(e.message!!.equals("io.takamaka.code.verification.IncompleteClasspathError: java.lang.ClassNotFoundException: io.takamaka.code.lang.Contract"))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun addJarStoreTransactionFailed() {
        RemoteNodeClient(url).use { client ->

            try {
                client.addJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        getGameteNonce(),
                        this.takamakaCode,
                        this.chainId,
                        "20000",
                        "1",
                        getJarExampleOf("callernotonthis"),
                        listOf(this.takamakaCode)
                    )
                )
            } catch (e: Exception) {
                assertTrue(e is TransactionException, "expected exception to of type TransactionRejectedException")
                assertTrue(e.message!!.contains("io.takamaka.code.verification.VerificationException"))
                assertTrue(e.message!!.contains("caller() can only be called on \"this\""))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun postJarStoreTransaction() {
        val jarTransaction: TransactionReferenceModel

        RemoteNodeClient(url).use { client ->

            val jarSupplier = client.postJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    getGameteNonce(),
                    this.takamakaCode,
                    this.chainId,
                    "20000",
                    "1",
                    getJarExampleOf("lambdas"),
                    listOf(this.takamakaCode)
                )
            )

            jarTransaction = jarSupplier.get()
        }

        assertNotNull(jarTransaction)
    }

    @Test
    fun postJarStoreTransactionRejected() {
        RemoteNodeClient(url).use { client ->

            try {
                // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
                // this means that the request fails and the future refers to a failed request; since this is a post,
                // the execution does not stop, nor throws anything
                val jarSupplier = client.postJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        getGameteNonce(),
                        this.takamakaCode,
                        this.chainId,
                        "20000",
                        "1",
                        getJarExampleOf("lambdas"),
                        listOf()
                    )
                )

                // we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
                // since the request failed and its transaction was rejected
                jarSupplier.get()

            } catch (e: Exception) {
                assertTrue(
                    e is TransactionRejectedException,
                    "expected exception to of type TransactionRejectedException"
                )
                assertTrue(e.message!!.equals("io.takamaka.code.verification.IncompleteClasspathError: java.lang.ClassNotFoundException: io.takamaka.code.lang.Contract"))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun postJarStoreTransactionFailed() {
        RemoteNodeClient(url).use { client ->

            try {

                // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
                // this means that the request fails and the future refers to a failed request; since this is a post,
                // the execution does not stop, nor throws anything
                val jarSupplier = client.postJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        getGameteNonce(),
                        this.takamakaCode,
                        this.chainId,
                        "20000",
                        "1",
                        getJarExampleOf("callernotonthis"),
                        listOf(this.takamakaCode)
                    )
                )

                jarSupplier.get()

            } catch (e: TransactionException) {
                assertTrue(e.message!!.contains("io.takamaka.code.verification.VerificationException"))
                assertTrue(e.message!!.contains("caller() can only be called on \"this\""))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun runStaticMethodCallTransaction() {
        val toString: StorageValueModel?

        RemoteNodeClient(url).use { client ->

            val jar = client.addJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    getGameteNonce(),
                    this.takamakaCode,
                    this.chainId,
                    "20000",
                    "1",
                    getJarExampleOf("javacollections"),
                    listOf(this.takamakaCode)
                )
            )

            val nonVoidMethodSignature = MethodSignatureModel(
                "testToString1",
                "java.lang.String",
                listOf(),
                "io.takamaka.tests.javacollections.HashMapTests"
            )

            toString = client.runStaticMethodCallTransaction(
                StaticMethodCallTransactionRequestModel(
                    "",
                    this.gamete,
                    getGameteNonce(),
                    jar,
                    this.chainId,
                    "20000",
                    "1",
                    nonVoidMethodSignature,
                    listOf()
                )
            )
        }

        assertEquals("[how, are, hello, you, ?]", toString?.value)
    }

    @Test
    fun runInstanceMethodCallTransaction() {
        val toString: StorageValueModel?

        RemoteNodeClient(url).use { client ->

            val nonVoidMethodSignature = MethodSignatureModel(
                "nonce",
                "java.math.BigInteger",
                listOf(),
                "io.takamaka.code.lang.Account"
            )

            toString = client.runInstanceMethodCallTransaction(
                InstanceMethodCallTransactionRequestModel(
                    "",
                    this.gamete,
                    getGameteNonce(),
                    this.takamakaCode,
                    this.chainId,
                    "20000",
                    "1",
                    nonVoidMethodSignature,
                    listOf(),
                    this.gamete
                )
            )
        }

        assertNotNull(toString)
        assertNotNull(toString?.value)
        val integerNonce = Integer.parseInt(toString?.value!!)
        assertTrue(integerNonce > 0)
    }

    @Test
    fun createFreeAccount() {
        RemoteNodeClient(url).use { client ->

            try {
                client.addRedGreenGameteCreationTransaction(
                    RedGreenGameteCreationTransactionRequestModel(
                        "10000",
                        "10000",
                        "",
                        this.takamakaCode
                    )
                )
            } catch (e: TransactionRejectedException) {
                assertTrue(e.message!!.equals("cannot run a RedGreenGameteCreationTransactionRequest in an already initialized node"))
                return
            }

            fail("expected exception")
        }
    }

    @Test
    fun stompClient() {

        val completableFuture = CompletableFuture<Boolean>()
        StompClient("$url/node").use { client ->

            client.connect(
                {

                    CompletableFuture.runAsync {

                        // subscribe to topic events
                        client.subscribeTo("/topic/events", EventRequestModel::class.java, { result, error ->

                            when {
                                error != null -> {
                                    fail("unexpected error")
                                }
                                result != null -> {
                                    val result = eventModel.event.transaction.hash == result.event.transaction.hash &&
                                            eventModel.creator.transaction.hash == result.creator.transaction.hash

                                    completableFuture.complete(result)
                                }
                                else -> {
                                    fail("unexpected payload")
                                }
                            }

                        }, {

                            // send message
                            client.sendTo("/events", eventModel)
                        })
                    }

                }, {
                    fail("Connection failed")
                }
            )

            assertTrue(completableFuture.get(4L, TimeUnit.SECONDS))
        }
    }

    @Test
    fun events() {
        val completableFuture = CompletableFuture<Boolean>()

        RemoteNodeClient(url).use { client ->

            // delayed task to send an event
            val delayedTask = CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS)

            CompletableFuture.runAsync {
                // subscribe to events topic
                client.subscribeToEvents(null) { event, key ->
                    val result = eventModel.event.transaction.hash == event.transaction.hash &&
                            eventModel.creator.transaction.hash == key.transaction.hash

                    completableFuture.complete(result)
                }

            }.thenRunAsync(
                {
                    // simulate an EVENT
                    val stompClient = StompClient("$url/node")
                    stompClient.connect({
                        stompClient.sendTo("/events", eventModel)
                    })
                },
                delayedTask
            )

            assertTrue(completableFuture.get(4L, TimeUnit.SECONDS))
        }
    }


    /**
     * It initializes the jar with the basic Takamaka classes along with a gamete and a manifest.
     */
    private fun initializeRemoteNode() {
        try {
            println("Initializing the remote node")

            this.takamakaCode = installTakamakaJar()
            this.gamete = createGamete(takamakaCode)
            this.manifest = createManifest(gamete, takamakaCode)
            installManifest(takamakaCode, manifest)

            println("Remote node initialized")
        } catch (e: Exception) {
            println("Remote node not initialized...exiting")
            e.printStackTrace()
            System.exit(0)
        }
    }

    private fun installTakamakaJar(): TransactionReferenceModel {
        RemoteNodeClient(url).use { client ->

            val jar = Base64.getEncoder()
                .encodeToString(Files.readAllBytes(Paths.get("../../modules/explicit/io-takamaka-code-${takamakaJarVersion}.jar")))
            return client.addJarStoreInitialTransaction(
                JarStoreInitialTransactionRequestModel(
                    jar,
                    listOf()
                )
            )
        }
    }

    private fun createGamete(takamakaCodeReference: TransactionReferenceModel): StorageReferenceModel {
        RemoteNodeClient(url).use { client ->

            return client.addRedGreenGameteCreationTransaction(
                RedGreenGameteCreationTransactionRequestModel(
                    "999999995000000009999999990000000004999999999",
                    "999999995000000009999999990000000004999999999",
                    "",
                    takamakaCodeReference
                )
            )
        }
    }

    private fun createManifest(
        gamete: StorageReferenceModel,
        takamakaCodeReference: TransactionReferenceModel
    ): StorageReferenceModel {
        RemoteNodeClient(url).use { client ->

            val gasLimit = "100000"
            val gasPrice = "0"

            val constructor = ConstructorSignatureModel(
                listOf("java.lang.String", "java.lang.String", "java.lang.String"),
                "io.takamaka.code.system.Manifest"
            )

            val actuals = listOf(
                StorageValueModel("java.lang.String", "io.takamaka.code.tests.TakamakaTest"),
                StorageValueModel("java.lang.String", ""),
                StorageValueModel("java.lang.String", "")
            )

            return client.addConstructorCallTransaction(
                ConstructorCallTransactionRequestModel(
                    "",
                    gamete,
                    "0",
                    takamakaCodeReference,
                    "",
                    gasLimit,
                    gasPrice,
                    constructor,
                    actuals
                )
            )
        }
    }

    private fun installManifest(
        takamakaCodeReference: TransactionReferenceModel,
        manifestReference: StorageReferenceModel
    ) {
        RemoteNodeClient(url).use { client ->

            client.addInitializationTransaction(
                InitializationTransactionRequestModel(
                    manifestReference,
                    takamakaCodeReference
                )
            )
        }
    }

    /**
     * It calls the nonce() method of the gamete and returns its value.
     * @return the nonce
     */
    private fun getGameteNonce(): String {
        val result: StorageValueModel?

        RemoteNodeClient(url).use { client ->

            val nonVoidMethodSignature = MethodSignatureModel(
                "nonce",
                "java.math.BigInteger",
                listOf(),
                "io.takamaka.code.lang.Account"
            )

            result = client.runInstanceMethodCallTransaction(
                InstanceMethodCallTransactionRequestModel(
                    "",
                    this.gamete,
                    "3",
                    this.takamakaCode,
                    this.chainId,
                    "20000",
                    "1",
                    nonVoidMethodSignature,
                    listOf(),
                    this.gamete
                )
            )
        }

        return if (result != null) result.value!! else "0"
    }

    private fun getJarExampleOf(name: String): String {
        return Base64.getEncoder()
            .encodeToString(Files.readAllBytes(Paths.get("../../io-takamaka-examples/target/io-takamaka-examples-${takamakaJarVersion}-${name}.jar")))
    }

    private fun getJarTestOf(name: String): String {
        return Base64.getEncoder()
            .encodeToString(Files.readAllBytes(Paths.get("../../io-takamaka-code-tests/jars/${name}.jar")))
    }
}