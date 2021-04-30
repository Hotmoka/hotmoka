/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.runs;

import java.nio.file.Paths;
import java.util.NoSuchElementException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;

/**
 * An example that shows how to create a brand new Tendermint Hotmoka blockchain.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.InitTendermintV1N1Node1
 */
public class InitTendermintV1N1Node1 extends Run {

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder()
			.setTendermintConfigurationToClone(Paths.get("io-hotmoka-runs/tendermint_configs/v1n1/node1"))
			.build();
		ConsensusParams consensus = new ConsensusParams.Builder().build();

		try (TendermintBlockchain node = TendermintBlockchain.init(config, consensus)) {
			printManifestWhenReady(node);
			pressEnterToExit();
		}
	}

	private static void printManifestWhenReady(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException, InterruptedException {
		while (true) {
			try {
				printManifest(node);
				return;
			}
			catch (NoSuchElementException e) {
				System.out.println("the manifest is not set yet");
			}

			Thread.sleep(1000);
		}
	}
}