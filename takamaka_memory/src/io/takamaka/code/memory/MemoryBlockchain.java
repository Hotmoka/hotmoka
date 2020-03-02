package io.takamaka.code.memory;

import java.io.IOException;
import java.nio.file.Path;

import io.hotmoka.nodes.SequentialNode;
import io.takamaka.code.memory.internal.MemoryBlockchainImpl;

/**
 * An implementation of a blockchain that stores, sequentially, transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 */
public interface MemoryBlockchain extends SequentialNode {

	/**
	 * The number of transactions per block.
	 */
	public final static short TRANSACTIONS_PER_BLOCK = 5;

	/**
	 * Yields a blockchain that stores transaction in disk memory.
	 * 
	 * @param root the directory where blocks and transactions must be stored.
	 * @throws IOException if the root directory cannot be created
	 */
	public static MemoryBlockchain of(Path root) throws IOException {
		return new MemoryBlockchainImpl(root);
	}
}