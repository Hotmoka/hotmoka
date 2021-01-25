package io.hotmoka.tendermint;

import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;
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
	 */
	static TendermintBlockchain init(TendermintBlockchainConfig config, ConsensusParams consensus) {
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
	 */
	static TendermintBlockchain restart(TendermintBlockchainConfig config) {
		return new TendermintBlockchainImpl(config);
	}
}