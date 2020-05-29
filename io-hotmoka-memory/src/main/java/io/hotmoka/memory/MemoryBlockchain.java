package io.hotmoka.memory;

import io.hotmoka.memory.internal.MemoryBlockchainImpl;
import io.hotmoka.nodes.NodeWithHistory;

/**
 * An implementation of a blockchain that stores, sequentially, transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 * It provides support for the creation of a given number of initial accounts.
 */
public interface MemoryBlockchain extends NodeWithHistory {

	/**
	 * Yields a blockchain in disk memory.
	 * 
	 * @param config the configuration of the blockchain
	 */
	static MemoryBlockchain of(Config config) {
		return new MemoryBlockchainImpl(config);
	}

}