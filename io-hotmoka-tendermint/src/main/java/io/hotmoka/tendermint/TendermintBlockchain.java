package io.hotmoka.tendermint;

import io.hotmoka.nodes.NodeWithRequestsAndResponses;
import io.hotmoka.tendermint.internal.TendermintBlockchainImpl;

/**
 * An implementation of a blockchain that relies on a Tendermint process.
 */
public interface TendermintBlockchain extends NodeWithRequestsAndResponses {

	/**
	 * Yields a Tendermint blockchain. This method spawns the Tendermint process and connects it to an ABCI application
	 * for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 */
	static TendermintBlockchain of(Config config) {
		return new TendermintBlockchainImpl(config);
	}
}