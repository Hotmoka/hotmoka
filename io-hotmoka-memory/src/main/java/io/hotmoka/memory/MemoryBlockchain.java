package io.hotmoka.memory;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Optional;

import io.hotmoka.memory.internal.MemoryBlockchainImpl;
import io.hotmoka.nodes.InitializedNode;

/**
 * An implementation of a blockchain that stores, sequentially, transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 * It provides support for the creation of a given number of initial accounts.
 */
public interface MemoryBlockchain extends InitializedNode {

	/**
	 * Yields a blockchain in disk memory and initializes user accounts with the given initial funds.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode()}
	 * @param funds the initial funds of the accounts that are created
	 * @throws Exception if the blockchain could not be created
	 */
	static MemoryBlockchain of(Config config, Path takamakaCodePath, BigInteger... funds) throws Exception {
		return new MemoryBlockchainImpl(config, takamakaCodePath, Optional.empty(), false, funds);
	}

	/**
	 * Yields a blockchain in disk memory and initializes red/green user accounts with the given initial funds.
	 * The only different with respect to {@linkplain #of(Path, BigInteger...)} is that the initial
	 * account are red/green externally owned accounts.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode()}
	 * @param funds the initial funds of the accounts that are created; they must be understood in pairs, each pair for the green/red
	 *              initial funds of each account (green before red)
	 * @throws Exception if the blockchain could not be created
	 */
	static MemoryBlockchain ofRedGreen(Config config, Path takamakaCodePath, BigInteger... funds) throws Exception {
		return new MemoryBlockchainImpl(config, takamakaCodePath, Optional.empty(), true, funds);
	}

	/**
	 * Yields a blockchain in disk memory and initializes user accounts with the given initial funds.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode()}
	 * @param jar the path of a user jar that must be installed. This is optional and mainly useful to simplify the implementation of tests
	 * @param funds the initial funds of the accounts that are created
	 * @throws Exception if the blockchain could not be created
	 */
	static MemoryBlockchain of(Config config, Path takamakaCodePath, Path jar, BigInteger... funds) throws Exception {
		return new MemoryBlockchainImpl(config, takamakaCodePath, Optional.of(jar), false, funds);
	}

	/**
	 * Yields a blockchain in disk memory and initializes red/green user accounts with the given initial funds.
	 * The only different with respect to {@linkplain #of(Path, BigInteger...)} is that the initial
	 * account are red/green externally owned accounts.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode()}
	 * @param funds the initial funds of the accounts that are created; they must be understood in pairs, each pair for the green/red
	 *              initial funds of each account (green before red)
	 * @throws Exception if the blockchain could not be created
	 */
	static MemoryBlockchain ofRedGreen(Config config, Path takamakaCodePath, Path jar, BigInteger... funds) throws Exception {
		return new MemoryBlockchainImpl(config, takamakaCodePath, Optional.of(jar), true, funds);
	}
}