
import io.hotmoka.network.thin.client.RemoteNode
import io.hotmoka.network.thin.client.internal.RemoteNodeClient
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test as test

class RemoteNodeTest {

    @test fun getTakamakaCode() {
        val nodeService : RemoteNode = RemoteNodeClient("localhost:8080")
        val takamakaCode = nodeService.getTakamakaCode();

        assertNotNull(takamakaCode.hash, "expected result to be not null")
        assertNotNull(takamakaCode.hash, "expected takamakaCode to be not null")
        assertEquals(takamakaCode.type, "local","expected takamakaCode type to be local")
    }
}