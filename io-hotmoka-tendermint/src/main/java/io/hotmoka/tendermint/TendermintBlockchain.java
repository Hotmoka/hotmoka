package io.hotmoka.tendermint;

import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.nodes.Node;
import io.hotmoka.tendermint.internal.TendermintBlockchainImpl;

/**
 * An implementation of a blockchain that relies on a Tendermint process.
 */
@ThreadSafe
public interface TendermintBlockchain extends Node {

	/**
	 * Yields the chain identifier of the underlying Tendermint blockchain.
	 * This is set when the Tendermint blockchain is started and can be specified
	 * in Tendermint's configuration file (otherwise, it is random).
	 * It needn't coincide with the chain id of the Hotmoka node itself,
	 * although this is a good idea, in general.
	 * 
	 * @return the chain identifier
	 */
	public String getTendermintChainId();

	/**
	 * Yields a Tendermint blockchain. This method spawns the Tendermint process and connects it to an ABCI application
	 * for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 */
	static TendermintBlockchain of(TendermintBlockchainConfig config) {
		return new TendermintBlockchainImpl(config);
	}
}