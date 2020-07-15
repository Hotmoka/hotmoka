package io.hotmoka.takamaka;

import io.hotmoka.nodes.NodeWithHistory;

/**
 * An implementation of a blockchain that relies on a Takamaka process.
 */
public interface TakamakaBlockchain extends NodeWithHistory {

	/**
	 * Yields a Takamaka blockchain. This method spawns the Takamaka process and connects it to an application
	 * for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 */
	static TakamakaBlockchain of(Config config) {
		return null;
	}
}