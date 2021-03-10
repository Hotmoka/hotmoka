package io.hotmoka.runs;

import java.math.BigInteger;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.nodes.ManifestHelper;
import io.hotmoka.nodes.Node;

/**
 * An example that shows how to create a brand new Tendermint Hotmoka blockchain.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartTendermintNode
 */
abstract class Run {

	/**
	 * Initial green stake.
	 */
	protected final static BigInteger GREEN = BigInteger.valueOf(10_000_000_000L).pow(5);

	/**
	 * Initial red stake.
	 */
	protected final static BigInteger RED = GREEN;

	protected final static BigInteger _10_000 = BigInteger.valueOf(10_000L);

	protected static void printManifest(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		System.out.println(new ManifestHelper(node));
	}

	protected static void pressEnterToExit() {
		System.out.println("\nPress enter to exit this program");
		System.console().readLine();
	}
}