package io.hotmoka.memory;

import java.io.IOException;
import java.nio.file.Path;

import io.hotmoka.beans.TransactionRejectedException;
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
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode()}
	 * @throws TransactionRejectedException if the initialization transaction that stores {@code takamakaCode} fails
	 * @throws IOException if {@code takamakaCode} cannot be accessed
	 */
	static MemoryBlockchain of(Config config, Path takamakaCodePath) throws TransactionRejectedException, IOException {
		return new MemoryBlockchainImpl(config, takamakaCodePath);
	}

}