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

import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.PrivateKey;

import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.views.TendermintInitializedNode;
import io.hotmoka.views.NodeWithAccounts;

/**
 * Creates a brand new blockchain and recreates it repeatedly, checking that the previous
 * state is available after each recreation.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.RestartTendermintNodeRepeatedly
 */
public class RestartTendermintNodeRepeatedly extends Run {
	private static final BigInteger _2_000_000_000 = BigInteger.valueOf(2_000_000_000);
	private static final BigInteger _100 = BigInteger.valueOf(100);

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder().build();
		ConsensusParams consensus = new ConsensusParams.Builder().build();
		StorageReference account;
		PrivateKey privateKey;
		StorageReference newAccount;

		try (TendermintBlockchain node = TendermintBlockchain.init(config, consensus)) {
			// update version number when needed
			TendermintInitializedNode initializedView = TendermintInitializedNode.of
				(node, consensus, Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar"), GREEN, RED);

			printManifest(node);
			NodeWithAccounts viewWithAccounts = NodeWithAccounts.of(initializedView, initializedView.gamete(), initializedView.keysOfGamete().getPrivate(), _2_000_000_000);
			System.out.println("takamakaCode: " + viewWithAccounts.getTakamakaCode());
			account = newAccount = viewWithAccounts.account(0);
			privateKey = viewWithAccounts.privateKey(0);
		}

		for (int i = 0; i < 100; i++)
			try (Node node = TendermintBlockchain.restart(config)) {
				// before creating a new account, we check if the previously created is still accessible
				node.getState(newAccount).forEach(System.out::println);
				newAccount = NodeWithAccounts.of(node, account, privateKey, _100).account(0);
				System.out.println("done #" + i);
			}
			catch (Exception e) {
				e.printStackTrace();
				break;
			}
	}
}