
import io.hotmoka.network.thin.client.RemoteNode
import io.hotmoka.network.thin.client.RemoteNodeClient
import io.hotmoka.network.thin.client.models.requests.EventRequestModel
import io.hotmoka.network.thin.client.models.requests.JarStoreInitialTransactionRequestModel
import io.hotmoka.network.thin.client.models.responses.JarStoreInitialTransactionResponseModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
import io.hotmoka.network.thin.client.webSockets.StompClient
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.timerTask
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Test as test

class RemoteNodeTest {
    //private val url = "ec2-54-194-239-91.eu-west-1.compute.amazonaws.com:8080"
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


    @test fun getTakamakaCode() {
        val nodeService : RemoteNode = RemoteNodeClient(url)
        val takamakaCode = nodeService.getTakamakaCode()

        assertNotNull(takamakaCode, "expected result to be not null")
        assertNotNull(takamakaCode.hash, "expected takamakaCode to be not null")
        assertEquals(takamakaCode.type, "local", "expected takamakaCode type to be local")
    }

    @test fun getManifest() {
        val nodeService : RemoteNode = RemoteNodeClient(url)
        val reference = nodeService.getManifest()

        assertNotNull(reference, "expected result to be not null")
        assertNotNull(reference.transaction, "expected transaction to be not null")
        assertEquals(reference.transaction.type, "local", "expected transaction reference to be of type local")
        assertNotNull(reference.transaction.hash, "expected transaction reference to be not null")
    }

    @test fun getState() {
        val nodeService : RemoteNode = RemoteNodeClient(url)
        val manifestReference = nodeService.getManifest()
        val state = nodeService.getState(manifestReference)

        assertNotNull(state, "expected state to be not null")
        assertEquals(state.updates.size, 2)
        assertNotNull(state.updates[0].updatedObject, "expected updateObject to not null")
        assertEquals(state.updates[0].updatedObject.transaction.hash, manifestReference.transaction.hash)
    }

    @test fun getStateNonExisting() {
        val nodeService : RemoteNode = RemoteNodeClient(url)

        try {
            nodeService.getState(nonExistingStorageReference)
        } catch (e: Exception) {
            assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
            assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
            return
        }

        fail("expected exception")
    }

    @test fun getClassTag() {
        val nodeService : RemoteNode = RemoteNodeClient(url)
        val manifestReference = nodeService.getManifest()
        val takamakaCode = nodeService.getTakamakaCode()
        val classTag = nodeService.getClassTag(manifestReference)

        assertNotNull(classTag, "expected classTag to be not null")
        assertEquals(classTag.className, "io.takamaka.code.system.Manifest")
        assertNotNull(classTag.jar.hash, "expected classTag jar to be not null")
        assertEquals(classTag.jar.hash, takamakaCode.hash, "expected classTag jar to be eq to the takamakaCode")
    }

    @test fun getClassTagNonExisting() {
        val nodeService : RemoteNode = RemoteNodeClient(url)

        try {
            nodeService.getClassTag(nonExistingStorageReference)
        } catch (e: Exception) {
            assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
            assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
            return
        }

        fail("expected exception")
    }

    @test fun getRequest() {
        val nodeService : RemoteNode = RemoteNodeClient(url)
        val takamakaCodeTransactionReference = nodeService.getTakamakaCode()
        val transactionRequest = nodeService.getRequest(takamakaCodeTransactionReference)

        assertNotNull(transactionRequest, "expected transactionRequest to be not null")
        assertTrue(
            transactionRequest.transactionResponseModel is JarStoreInitialTransactionRequestModel,
            "expected transaction request model to be of type JarStoreInitialTransactionResponseModel"
        )
    }


    @test fun getRequestNonExisting() {
        val nodeService: RemoteNode = RemoteNodeClient(url)

        try {
            nodeService.getRequest(nonExistingTransactionReference)
        } catch (e: Exception) {
            assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
            assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
            return
        }

        fail("expected exception")
    }


    @test fun getResponse() {
        val nodeService : RemoteNode = RemoteNodeClient(url)
        val takamakaCodeTransactionReference = nodeService.getTakamakaCode()
        val transactionResponse = nodeService.getResponse(takamakaCodeTransactionReference)

        assertNotNull(transactionResponse, "expected transactionResponse to be not null")
        assertTrue(
            transactionResponse.transactionResponseModel is JarStoreInitialTransactionResponseModel,
            "expected transaction response model to be of type JarStoreInitialTransactionResponseModel"
        )
    }

    @test fun getResponseNonExisting() {
        val nodeService : RemoteNode = RemoteNodeClient(url)

        try {
            nodeService.getRequest(nonExistingTransactionReference)
        } catch (e: Exception) {
            assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
            assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
            return
        }

        fail("expected exception")
    }

    @test fun getPolledResponseNonExisting() {
        val nodeService : RemoteNode = RemoteNodeClient(url)

        try {
            nodeService.getPolledResponse(nonExistingTransactionReference)
        } catch (e: Exception) {
            assertTrue(e is TimeoutException, "expected exception to of type TimeoutException")
            return
        }

        fail("expected exception")
    }

   /* @test fun addJarStoreTransaction() {
        val nodeService : RemoteNode = RemoteNodeClient(url)

        val signature = ""
        val caller = ...
        try {
            nodeService.addJarStoreTransaction(JarStoreTransactionRequestModel(

            ))
        } catch (e: Exception) {
            assertTrue(e is TimeoutException, "expected exception to of type TimeoutException")
            return
        }

        fail("expected exception")
    }*/


    @test fun stompClient() {

        val completableFuture = CompletableFuture<Boolean>()
        val stompClient = StompClient("localhost:8080/node")
        stompClient.use { client ->

            client.connect(
                {
                    client.subscribeTo("/topic/events", EventRequestModel::class.java) { result, error ->

                        when {
                            error != null -> {
                                fail("unexpected error")
                            }
                            result != null -> {
                                println("got result")

                                assertEquals(eventModel.event.transaction.hash, result.event.transaction.hash)
                                assertEquals(eventModel.key.transaction.hash, result.key.transaction.hash)
                                completableFuture.complete(true)
                            }
                            else -> {
                                fail("unexpected payload")
                            }
                        }
                    }


                    Timer().schedule(timerTask {
                        client.sendTo("/events", eventModel)
                    }, 2000)

                }, {
                    fail("Connection failed")
                }, {
                    println("Connection close")
                }
            )

            assertTrue(completableFuture.get(4L, TimeUnit.SECONDS))
        }
    }


    @test fun events() {
        val completableFuture = CompletableFuture<Boolean>()

        val nodeService : RemoteNode = RemoteNodeClient(url)
        nodeService.use { nodeService_ ->

            nodeService_.subscribeToEvents(null) { event, key ->
                assertEquals(eventModel.event.transaction.hash, event.transaction.hash)
                assertEquals(eventModel.key.transaction.hash, key.transaction.hash)
                completableFuture.complete(true)
            }

            Timer().schedule(timerTask {
                // simulate and EVENT
                val stompClient = StompClient("$url/node")
                stompClient.connect({
                    stompClient.sendTo("/events", eventModel)
                })
            }, 2000)

            assertTrue(completableFuture.get(4L, TimeUnit.SECONDS))
        }
    }

}