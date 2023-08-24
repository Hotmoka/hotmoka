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

package io.hotmoka.tendermint;

import java.io.IOException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.api.Node;
import io.hotmoka.tendermint.internal.TendermintBlockchainImpl;

/**
 * An implementation of a blockchain that relies on a Tendermint process.
 */
@ThreadSafe
public interface TendermintBlockchain extends Node {

	/**
	 * Yields the configuration of this node.
	 * 
	 * @return the configuration
	 */
	TendermintBlockchainConfig getConfig();

	/**
	 * Starts a Tendermint blockchain with a brand new store.
	 * This method spawns the Tendermint process and connects it to an ABCI application
	 * for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 * @param consensus the consensus parameters at the beginning of the life of the blockchain;
	 *                  when creating a node that starts synchronization with an already
	 *                  existing network, these must be the parameters at the beginning of the
	 *                  history of the network
	 * @return the Tendermint blockchain
	 * @throws IOException 
	 */
	static TendermintBlockchain init(TendermintBlockchainConfig config, ConsensusParams consensus) throws IOException {
		return new TendermintBlockchainImpl(config, consensus);
	}

	/**
	 * Starts a Tendermint blockchain that uses an already existing store. The consensus
	 * parameters are recovered from the manifest in the store, hence the store must
	 * be that of an already initialized blockchain.
	 * This method spawns the Tendermint process and connects it to an ABCI application
	 * for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 * @return the Tendermint blockchain
	 * @throws IOException 
	 */
	static TendermintBlockchain resume(TendermintBlockchainConfig config) throws IOException {
		return new TendermintBlockchainImpl(config);
	}
}