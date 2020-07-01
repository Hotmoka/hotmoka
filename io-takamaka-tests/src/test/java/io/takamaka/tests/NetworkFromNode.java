/**
 * 
 */
package io.takamaka.tests;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
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

	@Test @DisplayName("starts a network server from a Hotmoka node") @Disabled
	void startNetworkFromNode() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, InterruptedException {
		Config config = new Config.Builder().setPort(8080).setSpringBannerModeOn(true).build();
		NodeService nodeRestService = new NodeService(config, nodeWithJarsView);
		nodeRestService.start();
		nodeRestService.stop();
	}
}