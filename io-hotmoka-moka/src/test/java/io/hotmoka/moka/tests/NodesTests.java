/*
Copyright 2025 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.moka.tests;

import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.disk.DiskNodeConfigBuilders;
import io.hotmoka.node.disk.DiskNodes;
import io.hotmoka.node.service.NodeServices;
import io.takamaka.code.constants.Constants;

/**
 * Tests for the moka nodes command.
 */
public class NodesTests extends AbstractMokaTest {
	public final static String passwordOfGamete = "password";
	public static KeyPair keysOfGamete;
	public static Node node;
	public static StorageReference gamete;

	@BeforeAll
	public static void beforeAll(@TempDir Path dir) throws Exception {
		var nodeConfig = DiskNodeConfigBuilders.defaults().setDir(dir.resolve("chain")).build();
		var signature = SignatureAlgorithms.ed25519();
		var entropy = Entropies.random();
		keysOfGamete = entropy.keys(passwordOfGamete, signature);

		ConsensusConfig<?, ?> consensus = ConsensusConfigBuilders.defaults(signature)
			.allowUnsignedFaucet(true)
			.setInitialGasPrice(BigInteger.valueOf(100L))
			.setSignatureForRequests(signature)
			.setChainId("mryia")
			.setInitialSupply(BigInteger.valueOf(1000000000000000000L))
			.setFinalSupply(BigInteger.valueOf(2000000000000000000L))
			.setPublicKeyOfGamete(keysOfGamete.getPublic())
			.build();

		node = DiskNodes.init(nodeConfig);
		var takamakaCode = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();
		gamete = InitializedNodes.of(node, consensus, takamakaCode).gamete();
		entropy.dump(dir.resolve(gamete.toString() + ".pem")); // we save the entropy in a file named as the address of the gamete, that is, as an account
		NodeServices.of(node, 8001);
	}
	
	@Test
	@DisplayName("description")
	public void test(@TempDir Path dir) throws Exception {
	}

	@AfterAll
	public static void afterAll() throws Exception {
		node.close();
	}
}