package io.hotmoka.tendermint;

import java.math.BigInteger;
import java.nio.file.Paths;

import io.hotmoka.beans.TransactionException;

/**
 * Creates a brand new blockchain.
 */
public class Main {
	public static void main(String[] args) throws Exception {
		Config config = new Config(Paths.get("chain"), 26657, 26658);

		try (TendermintBlockchain blockchain = TendermintBlockchain.of
				(config, Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"), BigInteger.valueOf(200_000), BigInteger.valueOf(200_000), BigInteger.valueOf(200_000))) {

		}
		catch (Throwable e) {
			if (e instanceof TransactionException)
				e = e.getCause();

			e.printStackTrace();
		}
	}
}