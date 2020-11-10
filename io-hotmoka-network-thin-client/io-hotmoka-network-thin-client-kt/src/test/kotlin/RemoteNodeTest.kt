
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
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Test as test

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
    private var account: StorageReferenceModel? = null
    private lateinit var gamete: StorageReferenceModel
    private lateinit var takamakaCodeReference: TransactionReferenceModel
    private lateinit var manifestReference: StorageReferenceModel


    init {
        initializeNode()
    }

    @test fun getTakamakaCode() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val takamakaCode = service.getTakamakaCode()

            assertNotNull(takamakaCode, "expected result to be not null")
            assertNotNull(takamakaCode.hash, "expected takamakaCode to be not null")
            assertEquals(takamakaCode.type, "local", "expected takamakaCode type to be local")
        }
    }

    @test fun getManifest() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val reference = service.getManifest()

            assertNotNull(reference, "expected result to be not null")
            assertNotNull(reference.transaction, "expected transaction to be not null")
            assertEquals(reference.transaction.type, "local", "expected transaction reference to be of type local")
            assertNotNull(reference.transaction.hash, "expected transaction reference to be not null")
        }
    }

    @test fun getState() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val manifestReference = service.getManifest()
            val state = service.getState(manifestReference)

            assertNotNull(state, "expected state to be not null")
            assertEquals(state.updates.size, 2)
            assertNotNull(state.updates[0].updatedObject, "expected updateObject to not null")
            assertEquals(state.updates[0].updatedObject.transaction.hash, manifestReference.transaction.hash)
        }
    }

    @test fun getStateNonExisting() {
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

    @test fun getClassTag() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val manifestReference = service.getManifest()
            val takamakaCode = service.getTakamakaCode()
            val classTag = service.getClassTag(manifestReference)

            assertNotNull(classTag, "expected classTag to be not null")
            assertEquals(classTag.className, "io.takamaka.code.system.Manifest")
            assertNotNull(classTag.jar.hash, "expected classTag jar to be not null")
            assertEquals(classTag.jar.hash, takamakaCode.hash, "expected classTag jar to be eq to the takamakaCode")
        }
    }

    @test fun getClassTagNonExisting() {
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

    @test fun getRequest() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val takamakaCodeTransactionReference = service.getTakamakaCode()
            val transactionRequest = service.getRequest(takamakaCodeTransactionReference)

            assertNotNull(transactionRequest, "expected transactionRequest to be not null")
            assertTrue(
                transactionRequest.transactionResponseModel is JarStoreInitialTransactionRequestModel,
                "expected transaction request model to be of type JarStoreInitialTransactionResponseModel"
            )
        }
    }


    @test fun getRequestNonExisting() {
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


    @test fun getResponse() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val takamakaCodeTransactionReference = service.getTakamakaCode()
            val transactionResponse = service.getResponse(takamakaCodeTransactionReference)

            assertNotNull(transactionResponse, "expected transactionResponse to be not null")
            assertTrue(
                transactionResponse.transactionResponseModel is JarStoreInitialTransactionResponseModel,
                "expected transaction response model to be of type JarStoreInitialTransactionResponseModel"
            )
        }
    }

    @test fun getResponseNonExisting() {
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

    @test fun getPolledResponseNonExisting() {
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

    @test fun addJarStoreInitialTransaction() {

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
                assertTrue(e is TransactionRejectedException, "expected exception to of type TransactionRejectedException")
                assertTrue(e.message!!.equals("cannot run a JarStoreInitialTransactionRequest in an already initialized node"))
                return
            }

            fail("expected exception")
        }
    }

    @test fun addJarStoreTransaction() {
        initAccount()

        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            val signature = "X/EvcS8XRR38TapZIxu/2qkmKcs1DZeYQcRQ/ErQ9FGNs1p+pzg/PGTlqhDdnVsF6DEXfj+X/ngWvWMfDKHBBQ=="
            val caller = StorageReferenceModel(
                TransactionReferenceModel("local", "d56369c8d1d7c7fe54599e3897126ae6fec3c05a1c3e3cc2a8aae00ea1a67c5c"),
                "0"
            )
            val nonce = "0"
            val chainId = "io.takamaka.code.tests.TakamakaTest"
            val jar = Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get("../../io-takamaka-code-tests/jars/c13.jar")))
            val takamakaCode = service.getTakamakaCode()

            val transaction = service.addJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    signature,
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


    @test fun stompClient() {

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


    @test fun events() {
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

    private fun initAccount() {
        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->
            this.account = service.addRedGreenGameteCreationTransaction(
                RedGreenGameteCreationTransactionRequestModel(
                    "10000",
                    "10000",
                    "MCowBQYDK2VwAyEAM5+Aa9f8/Y+0cGlzzvN0ye/2Zs6O4LpI3nmkJe9uofU=",
                    service.getTakamakaCode()
                )
            )
        }
    }


    private fun initializeNode() {
        try {
            println("Initialized the remote node")

            this.takamakaCodeReference = installTakamakaJar()
            this.gamete = createGamete(takamakaCodeReference)
            this.manifestReference = createManifest(gamete, takamakaCodeReference)

            println("Remote node initialized")
        } catch (e: Exception) {
            println("Remote node not initialized...exiting")
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

            val gametePublicKey = "MCowBQYDK2VwAyEAPPbKrwOOHWWy3bxie8Zn6XVb3byh3LRKxdTLOPudZ0c="
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

            val gameteSignature = "hk3qUdakkzv3S/oKaBr7/lJIUAx/9oRWBv9Bel9r2ffjPi+TXRHKncWM9+x0g0pk1K0HHaG6NT5HjnLAGg2WAg=="
            val gasLimit = "100000"
            val gasPrice = "0"

            val constructor = ConstructorSignatureModel(
                listOf("java.lang.String", "java.lang.String", "java.lang.String"),
                "io.takamaka.code.system.Manifest"
            )

            val actuals = listOf(
                StorageValueModel("java.lang.String", ""),
                StorageValueModel("java.lang.String", ""),
                StorageValueModel("java.lang.String", "")
            )

            return service.addConstructorCallTransaction(
                ConstructorCallTransactionRequestModel(
                    gameteSignature,
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
}