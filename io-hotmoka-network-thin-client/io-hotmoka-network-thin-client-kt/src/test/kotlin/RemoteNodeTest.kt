
import io.hotmoka.network.thin.client.RemoteNode
import io.hotmoka.network.thin.client.RemoteNodeClient
import io.hotmoka.network.thin.client.exceptions.TransactionRejectedException
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.JarStoreInitialTransactionResponseModel
import io.hotmoka.network.thin.client.models.signatures.ConstructorSignatureModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
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
class RemoteNodeTest {
    private val url = "localhost:8080"
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

    private val gametePublicKey = "MCowBQYDK2VwAyEAPPbKrwOOHWWy3bxie8Zn6XVb3byh3LRKxdTLOPudZ0c="
    private lateinit var takamakaCodeReference: TransactionReferenceModel
    private lateinit var gamete: StorageReferenceModel
    private lateinit var manifestReference: StorageReferenceModel


    init {
        initializeRemoteNode()
    }



    @Test fun getTakamakaCode() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val takamakaCode = service.getTakamakaCode()

            assertNotNull(takamakaCode, "expected takamakaCode to be not null")
            assertEquals(this.takamakaCodeReference.hash, takamakaCode.hash)
            assertEquals(this.takamakaCodeReference.type, takamakaCode.type)
            assertEquals("local", this.takamakaCodeReference.type)
        }
    }

    @Test fun getManifest() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val reference = service.getManifest()

            assertNotNull(reference, "expected result to be not null")
            assertNotNull(reference.transaction, "expected transaction to be not null")
            assertEquals(this.manifestReference.transaction.hash, reference.transaction.hash)
            assertEquals(this.manifestReference.transaction.type, reference.transaction.type)
            assertEquals("local", reference.transaction.type)
        }
    }

    @Test fun getState() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val manifestReference = service.getManifest()
            val state = service.getState(manifestReference)

            assertNotNull(state, "expected state to be not null")
            assertEquals(2, state.updates.size)
            assertNotNull(state.updates[0].updatedObject, "expected updateObject to not null")
            assertEquals(this.manifestReference.transaction.hash, state.updates[0].updatedObject.transaction.hash)
        }
    }

    @Test fun getStateNonExisting() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.getState(nonExistingStorageReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun getClassTag() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val manifestReference = service.getManifest()
            val classTag = service.getClassTag(manifestReference)

            assertNotNull(classTag, "expected classTag to be not null")
            assertEquals("io.takamaka.code.system.Manifest", classTag.className)
            assertNotNull(classTag.jar.hash, "expected classTag jar to be not null")
            assertEquals(this.takamakaCodeReference.hash, classTag.jar.hash)
        }
    }

    @Test fun getClassTagNonExisting() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.getClassTag(nonExistingStorageReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun getRequest() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val transactionRequest = service.getRequest(this.takamakaCodeReference)

            assertNotNull(transactionRequest, "expected transactionRequest to be not null")
            assertTrue(
                transactionRequest.transactionResponseModel is JarStoreInitialTransactionRequestModel,
                "expected transaction request model to be of type JarStoreInitialTransactionResponseModel"
            )
        }
    }


    @Test fun getRequestNonExisting() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.getRequest(nonExistingTransactionReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }


    @Test fun getResponse() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val transactionResponse = service.getResponse(this.takamakaCodeReference)

            assertNotNull(transactionResponse, "expected transactionResponse to be not null")
            assertTrue(
                transactionResponse.transactionResponseModel is JarStoreInitialTransactionResponseModel,
                "expected transaction response model to be of type JarStoreInitialTransactionResponseModel"
            )
        }
    }

    @Test fun getResponseNonExisting() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.getRequest(nonExistingTransactionReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun getPolledResponseNonExisting() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.getPolledResponse(nonExistingTransactionReference)
            } catch (e: Exception) {
                assertTrue(e is TimeoutException, "expected exception to of type TimeoutException")
                return
            }

            fail("expected exception")
        }
    }

    @Test fun addJarStoreInitialTransaction() {

        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                val jar = Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get("../../io-takamaka-code-tests/jars/c13.jar")))

                service.addJarStoreInitialTransaction(
                    JarStoreInitialTransactionRequestModel(
                        jar,
                        listOf(service.getTakamakaCode())
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

        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            val caller = this.gamete
            val nonce = "1"
            val chainId = "io.takamaka.code.tests.TakamakaTest"
            val jar = Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get("../../io-takamaka-code-tests/jars/c13.jar")))
            val takamakaCode = service.getTakamakaCode()

            val transaction = service.addJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    caller,
                    nonce,
                    takamakaCode,
                    chainId,
                    "20000",
                    "1",
                    jar,
                    listOf(takamakaCode)
                )
            )


           assertNotNull(transaction)
        }
    }


    @Test fun createFreeAccount() {
        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.addRedGreenGameteCreationTransaction(
                    RedGreenGameteCreationTransactionRequestModel(
                        "10000",
                        "10000",
                        "MCowBQYDK2VwAyEAM5+Aa9f8/Y+0cGlzzvN0ye/2Zs6O4LpI3nmkJe9uofU=",
                        service.getTakamakaCode()
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
        val stompClient = StompClient("$url/node")
        stompClient.use { client ->

            client.connect(
                {

                    CompletableFuture.runAsync {

                        // subscribe
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

        val nodeService : RemoteNode = RemoteNodeClient(url)
        nodeService.use { nodeService_ ->

            val delayedTask = CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS)
            CompletableFuture.runAsync {
                nodeService_.subscribeToEvents(null) { event, key ->
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
            println("Initialized the remote node")

            this.takamakaCodeReference = installTakamakaJar()
            this.gamete = createGamete(takamakaCodeReference)
            this.manifestReference = createManifest(gamete, takamakaCodeReference)
            installManifest(takamakaCodeReference, manifestReference)

            println("Remote node initialized")
        } catch (e: Exception) {
            println("Remote node not initialized...exiting")
            e.printStackTrace()
            System.exit(0)
        }
    }

    private fun installTakamakaJar(): TransactionReferenceModel {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val jar = Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get("../../modules/explicit/io-takamaka-code-1.0.0.jar")))
            return service.addJarStoreInitialTransaction(
                JarStoreInitialTransactionRequestModel(
                    jar,
                    listOf()
                )
            )
        }
    }

    private fun createGamete(takamakaCodeReference: TransactionReferenceModel): StorageReferenceModel {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            return service.addRedGreenGameteCreationTransaction(
                RedGreenGameteCreationTransactionRequestModel(
                    "999999995000000009999999990000000004999999999",
                    "999999995000000009999999990000000004999999999",
                    gametePublicKey,
                    takamakaCodeReference
                )
            )
        }
    }

    private fun createManifest(gamete: StorageReferenceModel, takamakaCodeReference: TransactionReferenceModel): StorageReferenceModel {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

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

            return service.addConstructorCallTransaction(
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
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            service.addInitializationTransaction(
                InitializationTransactionRequestModel(
                    manifestReference,
                    takamakaCodeReference
                )
            )
        }
    }
}