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
import org.junit.jupiter.api.io.TempDir;

import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.moka.Moka;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.disk.DiskInitializedNodes;
import io.hotmoka.node.disk.DiskNodeConfigBuilders;
import io.hotmoka.node.disk.DiskNodes;
import io.hotmoka.node.service.NodeServices;
import io.takamaka.code.constants.Constants;

/**
 * Shared code of the tests for the moka tool that need a running node.
 */
public abstract class AbstractMokaTestWithNode extends AbstractMokaTest {
	public final static String passwordOfGamete = "password";
	
	/**
	 * The port where the test node gets published.
	 */
	public final static int PORT = 8000;

	public static Node node;

	/**
	 * The directory where the chain and the keys are saved.
	 */
	public static Path dir;

	public static StorageReference gamete;
	public static TransactionReference takamakaCode; 
	public static KeyPair keysOfGamete;

	@BeforeAll
	public static void beforeAll(@TempDir Path dir) throws Exception {
		AbstractMokaTestWithNode.dir = dir;
		var signature = SignatureAlgorithms.ed25519();
		var entropy = Entropies.random();
		keysOfGamete = entropy.keys(passwordOfGamete, signature);
		var takamakaCodePath = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();

		/*
		var nodeConfig = TendermintNodeConfigBuilders.defaults().setDir(dir.resolve("chain")).setTendermintConfigurationToClone(Paths.get("src", "test", "resources", "tendermint_configs", "v1n0", "node0")).build();
		var consensus = ValidatorsConsensusConfigBuilders.defaults(signature)
		.allowUnsignedFaucet(true)
		.setInitialGasPrice(BigInteger.valueOf(100L))
		.setSignatureForRequests(signature)
		.setChainId("mryia")
		.setInitialSupply(BigInteger.valueOf(4000000000000000000L))
		.setFinalSupply(BigInteger.valueOf(8000000000000000000L))
		.setPublicKeyOfGamete(keysOfGamete.getPublic())
		.build();
		var node = TendermintNodes.init(nodeConfig);
		var init = MokamintInitializedNodes.of(node, consensus, takamakaCodePath);
		*/

		var nodeConfig = DiskNodeConfigBuilders.defaults().setDir(dir.resolve("chain")).build();
		var consensus = ConsensusConfigBuilders.defaults(signature)
				.allowUnsignedFaucet(true)
				.setInitialGasPrice(BigInteger.valueOf(100L))
				.setSignatureForRequests(signature)
				.setChainId("mryia")
				.setInitialSupply(BigInteger.valueOf(4000000000000000000L))
				.setFinalSupply(BigInteger.valueOf(8000000000000000000L))
				.setPublicKeyOfGamete(keysOfGamete.getPublic())
				.build();
		var node = DiskNodes.init(nodeConfig);
		var init = DiskInitializedNodes.of(node, consensus, takamakaCodePath);

		AbstractMokaTestWithNode.node = node;
		gamete = init.gamete();
		takamakaCode = node.getTakamakaCode();
		entropy.dump(dir.resolve(gamete + ".pem")); // we save the entropy in a file named as the address of the gamete, that is, as an account
		NodeServices.of(node, PORT);
		// the faucet is opened at initialization time, but we still need to set its threshold
		Moka.nodesFaucet("10000000000 --dir=" + dir + " --password=" + passwordOfGamete + " --uri=ws://localhost:" + PORT);
	}
	
	@AfterAll
	public static void afterAll() throws Exception {
		node.close(); // this will also close the service
	}
}