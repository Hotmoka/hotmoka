package io.hotmoka.tendermint;

import java.nio.file.Path;
import java.util.Optional;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.nodes.NodeWithHistory;
import io.hotmoka.tendermint.internal.TendermintBlockchainImpl;

/**
 * An implementation of a blockchain that relies on a Tendermint process.
 */
public interface TendermintBlockchain extends NodeWithHistory {

	/**
	 * Yields a fresh Tendermint blockchain.
	 * This method spawns the Tendermint process and connects it to an ABCI application
	 * for handling its transactions. The blockchain gets deleted if it existed already at the given directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCode the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@linkplain #takamakaCode()}
	 * @throws TransactionRejectedException if some initialization transaction failed
	 */
	static TendermintBlockchain of(Config config, Path takamakaCode) throws TransactionRejectedException {
		return new TendermintBlockchainImpl(config, Optional.of(takamakaCode));
	}

	/**
	 * Yields a Tendermint blockchain and initializes it with the information already
	 * existing at its configuration directory. This method can be used to
	 * recover a blockchain already created in the past, with all its information.
	 * A Tendermint blockchain must have been already successfully created at
	 * its configuration directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @throws TransactionRejectedException if some initialization transaction failed
	 */
	static TendermintBlockchain of(Config config) throws TransactionRejectedException {
		return new TendermintBlockchainImpl(config, Optional.empty());
	}
}