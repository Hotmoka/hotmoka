/**
 * 
 */
package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.network.Config;
import io.hotmoka.network.NodeService;

/**
 * A test for creating a network server from a Hotmoka node.
 */
class NetworkFromNode extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("abstractfail.jar", _20_000, _20_000);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node")
	void startNetworkFromNode() {
		Config config = new Config.Builder().setPort(8080).setSpringBannerModeOn(true).build();
		NodeService nodeRestService = new NodeService(config, nodeWithJarsView);
		nodeRestService.start();
		nodeRestService.stop();
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs getTakamakaCode()")
	void queryTakamakaCode() throws InterruptedException, IOException {
		Config config = new Config.Builder().setPort(8080).setSpringBannerModeOn(false).build();
		NodeService nodeRestService = new NodeService(config, nodeWithJarsView);
		nodeRestService.start();
		String answer = curl(new URL("http://localhost:8080/node/takamakaCode"));
		nodeRestService.stop();
		assertEquals("{\"hash\":\"" + nodeWithJarsView.getTakamakaCode().getHash() + "\"}", answer);
	}

	private String curl(URL url) throws IOException {
	    try (InputStream is = url.openStream();
	    	 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
	    	return br.lines().collect(Collectors.joining("\n"));
	    }
	}
}