package io.hotmoka.tendermint;

import java.nio.file.Path;

/**
 * The configuration of the Tendermint blockchain.
 */
public class Config {

	/**
	 * The port of the Tendermint process. This will be spawned on localhost.
	 */
	public final int tendermintPort;

	/**
	 * The port of the ABCI application. This will be spawned on localhost.
	 */
	public final int abciPort;

	/**
	 * The directory where blocks and state will be persisted.
	 */
	public final Path dir;

	/**
	 * Creates the configuration of the Tendermint blockchain
	 * 
	 * @param dir the directory where blocks and state will be persisted
	 * @param tendermintPort the port of the Tendermint process. This will be spawned on localhost
	 * @param abciPort the port of the ABCI application. This will be spawned on localhost
	 */
	public Config(Path dir, int tendermintPort, int abciPort) {
		this.dir = dir;
		this.tendermintPort = tendermintPort;
		this.abciPort = abciPort;
	}
}