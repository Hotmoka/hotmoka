
import io.hotmoka.network.thin.client.RemoteNode
import io.hotmoka.network.thin.client.RemoteNodeClient
import io.hotmoka.network.thin.client.models.signatures.MethodSignatureModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
import java.lang.Exception
import java.util.NoSuchElementException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Test as test

class RemoteNodeTest {

    @test fun getTakamakaCode() {
        val nodeService : RemoteNode = RemoteNodeClient("localhost:8080")
        val takamakaCode = nodeService.getTakamakaCode()

        assertNotNull(takamakaCode, "expected result to be not null")
        assertNotNull(takamakaCode.hash, "expected takamakaCode to be not null")
        assertEquals(takamakaCode.type, "local","expected takamakaCode type to be local")
    }

    @test fun getManifest() {
        val nodeService : RemoteNode = RemoteNodeClient("localhost:8080")
        val reference = nodeService.getManifest()

        assertNotNull(reference, "expected result to be not null")
        assertNotNull(reference.transaction, "expected transaction to be not null")
        assertEquals(reference.transaction.type, "local","expected transaction reference to be of type local")
        assertNotNull(reference.transaction.hash, "expected transaction reference to be not null")
    }

    @test fun getState() {
        val nodeService : RemoteNode = RemoteNodeClient("localhost:8080")
        val manifestReference = nodeService.getManifest()
        val state = nodeService.getState(manifestReference)

        assertNotNull(state, "expected state to be not null")
        assertEquals(state.updates.size, 2)
        assertNotNull(state.updates[0].updatedObject, "expected updateObject to not null")
        assertEquals(state.updates[0].updatedObject.transaction.hash, manifestReference.transaction.hash)
    }

    @test fun getStateNonExisting() {
        val nodeService : RemoteNode = RemoteNodeClient("localhost:8080")
        val nonExistingTransaction = TransactionReferenceModel("local", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")

        try {
            nodeService.getState(StorageReferenceModel(nonExistingTransaction, "2"))
        } catch (e: Exception) {
            assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
            assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
            return
        }

        fail("expected exception")
    }

    @test fun getClassTag() {
        val nodeService : RemoteNode = RemoteNodeClient("localhost:8080")
        val manifestReference = nodeService.getManifest()
        val takamakaCode = nodeService.getTakamakaCode()
        val classTag = nodeService.getClassTag(manifestReference)

        assertNotNull(classTag, "expected classTag to be not null")
        assertEquals(classTag.className, "io.takamaka.code.system.Manifest")
        assertNotNull(classTag.jar.hash, "expected classTag jar to be not null")
        assertEquals(classTag.jar.hash, takamakaCode.hash, "expected classTag jar to be eq to the takamakaCode")
    }

    /*
    @test fun getRequest() {
        val nodeService : RemoteNode = RemoteNodeClient("localhost:8080")
        val manifestReference = nodeService.getManifest()
        val transactionRequest = nodeService.getRequest(manifestReference.transaction)

        assertNotNull(transactionRequest, "expected transactionRequest to be not null")
    }*/

}