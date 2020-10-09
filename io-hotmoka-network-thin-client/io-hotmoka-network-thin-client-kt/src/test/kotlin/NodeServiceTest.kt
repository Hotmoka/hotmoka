
import io.hotmo.network.thin.client.NodeService
import io.hotmo.network.thin.client.internal.NodeServiceImpl
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test as test

class NodeServiceTest {

    @test fun getTakamakaCode() {
        val nodeService : NodeService = NodeServiceImpl();
        assertNotNull(nodeService.getTakamakaCode(), "expected takamakacode to be not null")
    }
}