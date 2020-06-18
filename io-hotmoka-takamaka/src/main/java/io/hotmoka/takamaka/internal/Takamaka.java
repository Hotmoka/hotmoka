package io.hotmoka.takamaka.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.hotmoka.beans.requests.TransactionRequest;

/**
 * A proxy object that connects to the Takamaka process, sends requests to it
 * and gets responses from it.
 */
class Takamaka implements AutoCloseable {

	/**
	 * The blockchain for which the Takamaka process works.
	 */
	private final TakamakaBlockchainImpl node;

	/**
	 * An object for JSON manipulation.
	 */
	private final Gson gson = new Gson();

	private final static Logger logger = LoggerFactory.getLogger(Takamaka.class);

	/**
	 * Spawns the Takamaka process and creates a proxy to it.
	 * 
	 * @param config the configuration of the blockchain
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if Takamaka did not spawn up in the expected time
	 * @throws InterruptedException if the current thread was interrupted while waiting for the Takamaka process to run
	 */
	Takamaka(TakamakaBlockchainImpl node) throws IOException, InterruptedException, TimeoutException {
		this.node = node;

		String takamakaHome = node.config.dir + "/blocks";

		if (node.config.delete) {
			// start new
		}
		else {
			// start
		}

		// wait until it is up and running?
		ping();

		logger.info("The Takamaka process is up and running");
	}

	@Override
	public void close() throws InterruptedException, IOException {
		// TODO

		logger.info("The Takamaka process has been shut down");
	}

	/**
	 * Yields the Hotmoka error in the Takamaka transaction with the given hash.
	 * 
	 * @param hash the hash of the transaction to look for
	 * @return the error, if any. If the transaction didn't commit or committed successfully,
	 *         the result is an empty optional
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 * @throws JsonSyntaxException if the Takamaka response didn't match the expected syntax
	 */
	Optional<String> getErrorMessage(String hash) throws JsonSyntaxException, IOException, TimeoutException, InterruptedException {
		// TODO
		return Optional.empty();
	}

	/**
	 * Yields the Hotmoka request specified in the Takamaka result for the Hotmoka
	 * transaction with the given hash.
	 * 
	 * @param hash the hash of the transaction to look for
	 * @return the Hotmoka transaction request
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the Takamaka request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the Takamaka request
	 * @throws JsonSyntaxException if the Takamaka response didn't match the expected syntax
	 * @throws ClassNotFoundException if the Takamaka response contained an object of unknown class
	 */
	Optional<TransactionRequest<?>> getRequest(String hash) throws JsonSyntaxException, IOException, TimeoutException, InterruptedException, ClassNotFoundException {
		String tx = null; // TODO

		byte[] decoded = Base64.getDecoder().decode(tx);
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded))) {
			return Optional.of(TransactionRequest.from(ois));
		}
	}

	/**
	 * Waits until the Takamaka process acknowledges a ping.
	 * 
	 * @throws IOException if it is not possible to connect to the Takamaka process
	 * @throws TimeoutException if tried many times, but never got a reply
	 * @throws InterruptedException if interrupted while pinging
	 */
	private void ping() throws TimeoutException, InterruptedException, IOException {
		for (int reconnections = 1; reconnections <= node.config.maxPingAttempts; reconnections++) {
			// TODO
		}
	
		throw new TimeoutException("Cannot connect to Takamaka process at " + url() + ". Tried " + node.config.maxPingAttempts + " times");
	}

	/**
	 * Yields the URL of the Takamaka process.
	 * 
	 * @return the URL
	 * @throws MalformedURLException if the URL is not well formed
	 */
	private URL url() throws MalformedURLException {
		return new URL("http://127.0.0.1:" + node.config.takamakaPort);
	}
}