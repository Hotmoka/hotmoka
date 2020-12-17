
import io.hotmoka.network.thin.client.RemoteNodeClient
import io.hotmoka.network.thin.client.exceptions.TransactionException
import io.hotmoka.network.thin.client.exceptions.TransactionRejectedException
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.JarStoreInitialTransactionResponseModel
import io.hotmoka.network.thin.client.models.responses.JarStoreTransactionSuccessfulResponseModel
import io.hotmoka.network.thin.client.models.responses.TransactionRestResponseModel
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
class InitializedRemoteNodeTest {
    private val url = "localhost:8080"
    private val takamakaJarVersion = "1.0.0"
    private val chainId = "io.hotmoka.runs.StartNetworkServiceWithInitializedMemoryNodeAndEmptySignature"
    private var nonce = 1

    /**
     * The gamete storage reference to set
     */
    private val gamete = StorageReferenceModel(
        TransactionReferenceModel("local", "e559657c328ba0c389e05ecb02c1db3f95055430a96c0b56f714f0a7838457a4"),
        "0"
    )

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



    @Test fun getTakamakaCode() {
        RemoteNodeClient(url).use { client ->

            val takamakaCode = client.getTakamakaCode()

            assertNotNull(takamakaCode, "expected takamakaCode to be not null")
            assertNotNull(takamakaCode.hash, "expected takamakaCode hash to be not null")
            assertEquals("local", takamakaCode.type)
        }
    }

    @Test fun getSignatureAlgorithmForRequests() {
        RemoteNodeClient(url).use { client ->

            val algorithm = client.getSignatureAlgorithmForRequests()

            assertNotNull(algorithm)
            assertEquals("empty", algorithm.algorithm)
        }
    }

    @Test fun getManifest() {
        RemoteNodeClient(url).use { client ->

            val manifest = client.getManifest()

            assertNotNull(manifest, "expected manifest to be not null")
            assertNotNull(manifest.transaction, "expected manifest transaction to be not null")
            assertEquals("local", manifest.transaction.type)
        }
    }

    @Test fun getState() {
        RemoteNodeClient(url).use { client ->

            val manifestReference = client.getManifest()
            val state = client.getState(manifestReference)

            assertNotNull(state, "expected state to be not null")
            assertEquals(2, state.updates.size)
            assertNotNull(state.updates[0].updatedObject, "expected updateObject to not null")
            assertEquals(manifestReference.transaction.hash, state.updates[0].updatedObject.transaction.hash)
        }
    }

    @Test fun getStateNonExisting() {
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

    @Test fun getClassTag() {
        RemoteNodeClient(url).use { client ->

            val manifestReference = client.getManifest()
            val classTag = client.getClassTag(manifestReference)

            assertNotNull(classTag, "expected classTag to be not null")
            assertEquals("io.takamaka.code.system.Manifest", classTag.className)
            assertNotNull(classTag.jar.hash, "expected classTag jar to be not null")
        }
    }

    @Test fun getClassTagNonExisting() {
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

    @Test fun getRequest() {
        RemoteNodeClient(url).use { client ->

            val transactionRequest = client.getRequest(client.getTakamakaCode())

            assertNotNull(transactionRequest, "expected transactionRequest to be not null")
            assertTrue(
                transactionRequest.transactionResponseModel is JarStoreInitialTransactionRequestModel,
                "expected transaction request model to be of type JarStoreInitialTransactionResponseModel"
            )
        }
    }

    @Test fun getRequestNonExisting() {
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

    @Test fun getResponse() {
        RemoteNodeClient(url).use { client ->

            val transactionResponse = client.getResponse(client.getTakamakaCode())

            assertNotNull(transactionResponse, "expected transactionResponse to be not null")
            assertTrue(
                transactionResponse.transactionResponseModel is JarStoreInitialTransactionResponseModel,
                "expected transaction response model to be of type JarStoreInitialTransactionResponseModel"
            )
        }
    }

    @Test fun getResponseNonExisting() {
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

    @Test fun getResponseFailed() {
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
                            getIncrementedNonceOfGamete(),
                            client.getTakamakaCode(),
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

    @Test fun getPolledResponse() {
        val transactionResponse: TransactionRestResponseModel<*>

        RemoteNodeClient(url).use { client ->

            val takamakaCodeRef = client.getTakamakaCode()

            val jarSupplier = client.postJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    nonce++.toString(),
                    takamakaCodeRef,
                    this.chainId,
                    "20000",
                    "1",
                    getJarExampleOf("lambdas"),
                    listOf(takamakaCodeRef)
                )
            )

            transactionResponse = client.getPolledResponse(jarSupplier.getReferenceOfRequest())
        }

        assertNotNull(transactionResponse)
        assertNotNull(transactionResponse.transactionResponseModel)
        assertTrue(transactionResponse.transactionResponseModel is JarStoreTransactionSuccessfulResponseModel)
    }

    @Test fun getPolledResponseNonExisting() {
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

    @Test fun getPolledResponseFailed() {
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
                            getIncrementedNonceOfGamete(),
                            client.getTakamakaCode(),
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

    @Test fun addJarStoreInitialTransaction() {
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

    @Test fun addJarStoreTransaction() {
        RemoteNodeClient(url).use { client ->

            val takamakaCode = client.getTakamakaCode()
            val transaction = client.addJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    nonce++.toString(),
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

    @Test fun addJarStoreTransactionRejected() {
        RemoteNodeClient(url).use { client ->

            val incorrectClasspath = TransactionReferenceModel("local", "")

            try {
                client.addJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        getIncrementedNonceOfGamete(),
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

    @Test fun addJarStoreTransactionFailed() {
        RemoteNodeClient(url).use { client ->

            val takamakaCodeRef = client.getTakamakaCode()
            try {
                client.addJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        nonce++.toString(),
                        takamakaCodeRef,
                        this.chainId,
                        "20000",
                        "1",
                        getJarExampleOf("callernotonthis"),
                        listOf(takamakaCodeRef)
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

    @Test fun postJarStoreTransaction() {
        val jarTransaction: TransactionReferenceModel

        RemoteNodeClient(url).use { client ->

            val takamakaCodeRef = client.getTakamakaCode()
            val jarSupplier = client.postJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    nonce++.toString(),
                    takamakaCodeRef,
                    this.chainId,
                    "20000",
                    "1",
                    getJarExampleOf("lambdas"),
                    listOf(takamakaCodeRef)
                )
            )

            jarTransaction = jarSupplier.get()
        }

        assertNotNull(jarTransaction)
    }

    @Test fun postJarStoreTransactionRejected() {
        RemoteNodeClient(url).use { client ->

            try {
                // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
                // this means that the request fails and the future refers to a failed request; since this is a post,
                // the execution does not stop, nor throws anything
                val jarSupplier = client.postJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        getIncrementedNonceOfGamete(),
                        client.getTakamakaCode(),
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

    @Test fun postJarStoreTransactionFailed() {
        RemoteNodeClient(url).use { client ->

            val takamakaCodeRef = client.getTakamakaCode()
            try {

                // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
                // this means that the request fails and the future refers to a failed request; since this is a post,
                // the execution does not stop, nor throws anything
                val jarSupplier = client.postJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        nonce++.toString(),
                        takamakaCodeRef,
                        this.chainId,
                        "20000",
                        "1",
                        getJarExampleOf("callernotonthis"),
                        listOf(takamakaCodeRef)
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

    @Test fun runStaticMethodCallTransaction() {
        val toString: StorageValueModel?

        RemoteNodeClient(url).use { client ->

            val takamakaCodeRef = client.getTakamakaCode()
            val jar = client.addJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    nonce++.toString(),
                    takamakaCodeRef,
                    this.chainId,
                    "20000",
                    "1",
                    getJarExampleOf("javacollections"),
                    listOf(takamakaCodeRef)
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
                    getIncrementedNonceOfGamete(),
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

    @Test fun runInstanceMethodCallTransaction() {
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
                    getIncrementedNonceOfGamete(),
                    client.getTakamakaCode(),
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

    @Test fun createFreeAccount() {
        RemoteNodeClient(url).use { client ->

            try {
                client.addRedGreenGameteCreationTransaction(
                    RedGreenGameteCreationTransactionRequestModel(
                        "10000",
                        "10000",
                        "",
                        client.getTakamakaCode()
                    )
                )
            } catch (e: TransactionRejectedException) {
                assertTrue(e.message!!.equals("cannot run a RedGreenGameteCreationTransactionRequest in an already initialized node"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun stompClient() {
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

    @Test fun events() {
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

    private fun getIncrementedNonceOfGamete(): String {
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
                    client.getTakamakaCode(),
                    this.chainId,
                    "20000",
                    "1",
                    nonVoidMethodSignature,
                    listOf(),
                    this.gamete
                )
            )
        }

        return if (result != null) "" + (Integer.parseInt(result.value!!) + 1) else "0"
    }

    private fun getJarExampleOf(name: String): String {
        return Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get("../../io-takamaka-examples/target/io-takamaka-examples-${takamakaJarVersion}-${name}.jar")))
    }

    private fun getJarTestOf(name: String): String {
        return Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get("../../io-takamaka-code-tests/jars/${name}.jar")))
    }
}