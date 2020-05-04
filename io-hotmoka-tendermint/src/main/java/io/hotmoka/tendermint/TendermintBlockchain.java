package io.hotmoka.tendermint;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Optional;

import io.hotmoka.nodes.InitializedNode;
import io.hotmoka.tendermint.internal.TendermintBlockchainImpl;

/**
 * An implementation of a blockchain that relies on a Tendermint process.
 */
public interface TendermintBlockchain extends InitializedNode {

	/**
	 * Yields a fresh Tendermint blockchain and initializes user accounts with the given initial funds.
	 * This method spawns the Tendermint process and connects it to an ABCI application
	 * for handling its transactions. The blockchain gets deleted if it existed already at the given directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@linkplain #takamakaCode()}
	 * @param funds the initial funds of the accounts that are created
	 * @throws Exception if the blockchain could not be created
	 */
	static TendermintBlockchain of(Config config, Path takamakaCodePath, BigInteger... funds) throws Exception {
		return new TendermintBlockchainImpl(config, takamakaCodePath, Optional.empty(), false, funds);
	}

	/**
	 * Yields a fresh Tendermint blockchain and initializes red/green user accounts with the given initial funds.
	 * The only different with respect to {@linkplain #of(Config, Path, BigInteger...)} is that the initial
	 * account are red/green externally owned accounts.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@linkplain #takamakaCode()}
	 * @param funds the initial funds of the accounts that are created; they must be understood in pairs, each pair for the green/red
	 *              initial funds of each account (green before red)
	 * @throws Exception if the blockchain could not be created
	 */
	static TendermintBlockchain ofRedGreen(Config config, Path takamakaCodePath, BigInteger... funds) throws Exception {
		return new TendermintBlockchainImpl(config, takamakaCodePath, Optional.empty(), true, funds);
	}

	/**
	 * Yields a fresh Tendermint blockchain and initializes user accounts with the given initial funds.
	 * This method spawns the Tendermint process on localhost and connects it to an ABCI application
	 * for handling its transactions. The blockchain gets deleted if it existed already at the given directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@linkplain #takamakaCode()}
	 * @param jar the path of a user jar that must be installed. This is optional and mainly useful to simplify the implementation of tests
	 * @param funds the initial funds of the accounts that are created
	 * @throws Exception if the blockchain could not be created
	 */
	static TendermintBlockchain of(Config config, Path takamakaCodePath, Path jar, BigInteger... funds) throws Exception {
		return new TendermintBlockchainImpl(config, takamakaCodePath, Optional.of(jar), false, funds);
	}

	/**
	 * Yields a fresh Tendermint blockchain and initializes red/green user accounts with the given initial funds.
	 * The only different with respect to {@linkplain #of(Config, Path, BigInteger...)} is that the initial
	 * account are red/green externally owned accounts.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@linkplain #takamakaCode()}
	 * @param jar the path of a user jar that must be installed. This is optional and mainly useful to simplify the implementation of tests
	 * @param funds the initial funds of the accounts that are created; they must be understood in pairs, each pair for the green/red
	 *              initial funds of each account (green before red)
	 * @throws Exception if the blockchain could not be created
	 */
	static TendermintBlockchain ofRedGreen(Config config, Path takamakaCodePath, Path jar, BigInteger... funds) throws Exception {
		return new TendermintBlockchainImpl(config, takamakaCodePath, Optional.of(jar), true, funds);
	}

	/**
	 * Yields a Tendermint blockchain and initializes it with the information already
	 * existing at its configuration directory. This method can be used to
	 * recover a blockchain already created in the past, with all its information.
	 * A Tendermint blockchain must have been already successfully created at
	 * its configuration directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @throws Exception if the blockchain could not be created
	 */
	static TendermintBlockchain of(Config config) throws Exception {
		return new TendermintBlockchainImpl(config);
	}
}