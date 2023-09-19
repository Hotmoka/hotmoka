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

package io.hotmoka.node.tendermint;

import java.io.IOException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.ConsensusConfig;
import io.hotmoka.node.tendermint.api.TendermintNode;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.node.tendermint.internal.TendermintNodeImpl;

/**
 * Providers of blockchain nodes that rely on a Tendermint process.
 */
@ThreadSafe
public abstract class TendermintNodes {

	private TendermintNodes() {}

	/**
	 * Creates and starts a node with a brand new store, of a blockchain based on Tendermint.
	 * It spawns the Tendermint process and connects it to an ABCI application for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 * @param consensus the consensus parameters at the beginning of the life of the blockchain;
	 *                  when creating a node that starts synchronization with an already
	 *                  existing network, these must be the parameters at the beginning of the
	 *                  history of the network
	 * @return the Tendermint node
	 * @throws IOException if an I/O error occurs
	 */
	public static TendermintNode init(TendermintNodeConfig config, ConsensusConfig consensus) throws IOException {
		return new TendermintNodeImpl(config, consensus);
	}

	/**
	 * Starts a Tendermint node that uses an already existing store. The consensus
	 * parameters are recovered from the manifest in the store, hence the store must
	 * be that of an already initialized blockchain. It spawns the Tendermint process
	 * and connects it to an ABCI application for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 * @return the Tendermint node
	 * @throws IOException if an I/O error occurs
	 */
	public static TendermintNode resume(TendermintNodeConfig config) throws IOException {
		return new TendermintNodeImpl(config);
	}
}