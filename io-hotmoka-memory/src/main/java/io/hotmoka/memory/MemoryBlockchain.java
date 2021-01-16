package io.hotmoka.memory;

import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.memory.internal.MemoryBlockchainImpl;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;

/**
 * An implementation of a blockchain that stores, sequentially, transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 * It provides support for the creation of a given number of initial accounts.
 */
@ThreadSafe
public interface MemoryBlockchain extends Node {

	/**
	 * Creates a brand new blockchain in disk memory.
	 * 
	 * @param config the configuration of the blockchain
	 * @return the blockchain
	 */
	static MemoryBlockchain init(MemoryBlockchainConfig config, ConsensusParams consensus) {
		return new MemoryBlockchainImpl(config, consensus);
	}
}