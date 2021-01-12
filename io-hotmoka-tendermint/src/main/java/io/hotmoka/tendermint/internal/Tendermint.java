package io.hotmoka.tendermint.internal;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.tendermint.TendermintValidator;
import io.hotmoka.tendermint.internal.beans.TendermintBroadcastTxResponse;
import io.hotmoka.tendermint.internal.beans.TendermintGenesisResponse;
import io.hotmoka.tendermint.internal.beans.TendermintTxResponse;
import io.hotmoka.tendermint.internal.beans.TendermintTxResult;
import io.hotmoka.tendermint.internal.beans.TendermintValidatorPriority;
import io.hotmoka.tendermint.internal.beans.TendermintValidatorsResponse;
import io.hotmoka.tendermint.internal.beans.TxError;

/**
 * A proxy object that connects to the Tendermint process, sends requests to it
 * and gets responses from it.
 */
class Tendermint implements AutoCloseable {

	/**
	 * The blockchain for which the Tendermint process works.
	 */
	private final TendermintBlockchainImpl node;

	/**
	 * The Tendermint process;
	 */
	private final Process process;

	/**
	 * An object for JSON manipulation.
	 */
	private final Gson gson = new Gson();

	private final static Logger logger = LoggerFactory.getLogger(Tendermint.class);

	/**
	 * Spawns the Tendermint process and creates a proxy to it. It assumes that
	 * the {@code tendermint} command can be executed from the command path.
	 * 
	 * @param node the blockchain that is using Tendermint
	 * @param deletePrevious true if and only if a previously existing working directory must
	 *                       be deleted and recreated; if false, its content gets recycled
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if Tendermint did not spawn up in the expected time
	 * @throws InterruptedException if the current thread was interrupted while waiting for the Tendermint process to run
	 */
	Tendermint(TendermintBlockchainImpl node, boolean deletePrevious) throws IOException, InterruptedException, TimeoutException {
		this.node = node;

		if (deletePrevious)
			initWorkingDirectoryOfTendermintProcess();

		this.process = spawnTendermintProcess();
		waitUntilTendermintProcessIsUp();

		logger.info("The Tendermint process is up and running");
	}

	@Override
	public void close() throws InterruptedException, IOException {
		// the following is important under Windows, since the shell script thats starts Tendermint
		// under Windows spawns it as a subprocess
		process.descendants().forEach(ProcessHandle::destroy);
		process.destroy();
		process.waitFor();

		if (System.getProperty("os.name").startsWith("Windows"))
			// this seems important under Windows
			try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				logger.info(br.lines().collect(Collectors.joining()));
			}

		logger.info("The Tendermint process has been shut down");
	}

	/**
	 * Sends the given {@code request} to the Tendermint process, inside a {@code broadcast_tx_async} Tendermint request.
	 * 
	 * @param request the request to send
	 * @return the response of Tendermint
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 */
	String broadcastTxAsync(TransactionRequest<?> request) throws IOException, TimeoutException, InterruptedException {
		String jsonTendermintRequest = "{\"method\": \"broadcast_tx_async\", \"params\": {\"tx\": \"" +  Base64.getEncoder().encodeToString(request.toByteArray()) + "\"}}";
		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Yields the Hotmoka error in the Tendermint transaction with the given hash.
	 * 
	 * @param hash the hash of the transaction to look for
	 * @return the error, if any. If the transaction didn't commit or committed successfully,
	 *         the result is an empty optional
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 * @throws JsonSyntaxException if the Tendermint response didn't match the expected syntax
	 */
	Optional<String> getErrorMessage(String hash) throws JsonSyntaxException, IOException, TimeoutException, InterruptedException {
		TendermintTxResponse response = gson.fromJson(tx(hash), TendermintTxResponse.class);

		if (response.error != null)
			// the Tendermint transaction didn't commit successfully
			return Optional.empty();
		else {
			// the Tendermint transaction committed successfully
			TendermintTxResult tx_result = response.result.tx_result;
			if (tx_result == null)
				throw new InternalFailureException("no result for Tendermint transacti)on " + hash);
			else if (tx_result.data != null && !tx_result.data.isEmpty())
				return Optional.of(new String(Base64.getDecoder().decode(tx_result.data)));
			else
				// there is no Hotmoka error in this transaction
				return Optional.empty();
		}
	}

	/**
	 * Yields the Hotmoka request specified in the Tendermint result for the Hotmoka
	 * transaction with the given hash.
	 * 
	 * @param hash the hash of the transaction to look for
	 * @return the Hotmoka transaction request
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the Tendermint request failed after trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the Tendermint request
	 * @throws JsonSyntaxException if the Tendermint response didn't match the expected syntax
	 * @throws ClassNotFoundException if the Tendermint response contained an object of unknown class
	 */
	Optional<TransactionRequest<?>> getRequest(String hash) throws JsonSyntaxException, IOException, TimeoutException, InterruptedException, ClassNotFoundException {
		TendermintTxResponse response = gson.fromJson(tx(hash), TendermintTxResponse.class);
		if (response.error != null)
			// the Tendermint transaction didn't commit successfully
			return Optional.empty();

		String tx = response.result.tx;
		if (tx == null)
			throw new InternalFailureException("no Hotmoka request in Tendermint response");

		byte[] decoded = Base64.getDecoder().decode(tx);
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded))) {
			return Optional.of(TransactionRequest.from(ois));
		}
	}

	/**
	 * Yields the chain identifier of the Tendermint network.
	 * 
	 * @return the chain identifier
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the Tendermint request failed after trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the Tendermint request
	 * @throws JsonSyntaxException if the Tendermint response didn't match the expected syntax
	 * @throws ClassNotFoundException if the Tendermint response contained an object of unknown class
	 */
	String getChainId() throws JsonSyntaxException, IOException, TimeoutException, InterruptedException {
		TendermintGenesisResponse response = gson.fromJson(genesis(), TendermintGenesisResponse.class);
		if (response.error != null)
			throw new InternalFailureException(response.error);

		String chainId = response.result.genesis.chain_id;
		if (chainId == null)
			throw new InternalFailureException("no chain id in Tendermint response");

		return chainId;
	}

	/**
	 * Yields the current validators of the Tendermint network.
	 * 
	 * @return the current validators
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the Tendermint request failed after trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the Tendermint request
	 * @throws JsonSyntaxException if the Tendermint response didn't match the expected syntax
	 * @throws ClassNotFoundException if the Tendermint response contained an object of unknown class
	 */
	Stream<TendermintValidator> getValidators() throws JsonSyntaxException, IOException, TimeoutException, InterruptedException {
		// the parameters of the validators() query seem to be ignored, no count nor total is returned
		String jsonResponse = validators(1, 100);
		TendermintValidatorsResponse response = gson.fromJson(jsonResponse, TendermintValidatorsResponse.class);
		if (response.error != null)
			throw new InternalFailureException(response.error);

		return response.result.validators.stream().map(Tendermint::intoTendermintValidator);
	}

	/**
	 * Checks if the response of Tendermint contains errors.
	 * 
	 * @param response the Tendermint response
	 */
	void checkBroadcastTxResponse(String response) {
		TendermintBroadcastTxResponse parsedResponse = gson.fromJson(response, TendermintBroadcastTxResponse.class);
	
		TxError error = parsedResponse.error;
		if (error != null)
			throw new InternalFailureException("Tendermint transaction failed: " + error.message + ": " + error.data);
	}

	/**
	 * Initialize the working directory for Tendermint.
	 * If that directory is required to be deleted on start-up (which is the default)
	 * there are two possibilities: either it clones a Tendermint configuration directory specified
	 * in the configuration of the node, or it creates a default Tendermint configuration
	 * with a single node, that acts as unique validator of the network.
	 */
	private void initWorkingDirectoryOfTendermintProcess() throws InterruptedException, IOException {
		if (node.config.tendermintConfigurationToClone == null) {
			// if there is no configuration to clone, we create a default network of a single node
			// that plays the role of unique validator of the network

			String tendermintHome = node.config.dir + File.separator + "blocks";
			//if (run("tendermint testnet --v 1 --o " + tendermintHome + " --populate-persistent-peers", Optional.empty()).waitFor() != 0)
			if (run("tendermint init --home " + tendermintHome, Optional.empty()).waitFor() != 0)
				throw new IOException("Tendermint initialization failed");
		}
		else
			// we clone the configuration files inside node.config.tendermintConfigurationToClone
			// into the directory tendermintHome
			copyRecursively(node.config.tendermintConfigurationToClone, node.config.dir.resolve("blocks"));
	}

	/**
	 * Spawns a Tendermint process using the working directory that has been previously initialized.
	 * 
	 * @return the Tendermint process
	 */
	private Process spawnTendermintProcess() throws IOException {
		// spawns a process that remains in background
		String tendermintHome = node.config.dir + File.separator + "blocks";
		//this.process = run("tendermint node --home " + tendermintHome + "/node0 --abci grpc --proxy_app tcp://127.0.0.1:" + node.config.abciPort, Optional.of("tendermint.log"));
		return run("tendermint node --home " + tendermintHome + " --abci grpc --proxy_app tcp://127.0.0.1:" + node.config.abciPort, Optional.of("tendermint.log"));
	}

	/**
	 * Waits until the Tendermint process acknowledges a ping.
	 * 
	 * @throws IOException if it is not possible to connect to the Tendermint process
	 * @throws TimeoutException if tried many times, but never got a reply
	 * @throws InterruptedException if interrupted while pinging
	 */
	private void waitUntilTendermintProcessIsUp() throws TimeoutException, InterruptedException, IOException {
		for (int reconnections = 1; reconnections <= node.config.maxPingAttempts; reconnections++) {
			try {
				HttpURLConnection connection = openPostConnectionToTendermint();
				try (OutputStream os = connection.getOutputStream(); InputStream is = connection.getInputStream()) {
					return;
				}
			}
			catch (ConnectException e) {
				// take a nap, then try again
				Thread.sleep(node.config.pingDelay);
			}
		}
	
		throw new TimeoutException("Cannot connect to Tendermint process at " + url() + ". Tried " + node.config.maxPingAttempts + " times");
	}

	private static void copyRecursively(Path src, Path dest) throws IOException {
	    try (Stream<Path> stream = Files.walk(src)) {
	        stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
	    }
	    catch (UncheckedIOException e) {
	    	throw e.getCause();
	    }
	}

	private static void copy(Path source, Path dest) {
		try {
			Files.copy(source, dest);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static TendermintValidator intoTendermintValidator(TendermintValidatorPriority validatorPriority) {
		if (validatorPriority.address == null)
			throw new InternalFailureException("unexpected null address in Tendermint validator");
		else if (validatorPriority.voting_power <= 0L)
			throw new InternalFailureException("unexpected non-positive voting power in Tendermint validator");
		else if (validatorPriority.pub_key.value == null)
			throw new InternalFailureException("unexpected null public key for Tendermint validator");
		else if (validatorPriority.pub_key.type == null)
			throw new InternalFailureException("unexpected null public key type for Tendermint validator");
		else
			return new TendermintValidator(validatorPriority.address, validatorPriority.voting_power, validatorPriority.pub_key.value, validatorPriority.pub_key.type);
	}

	/**
	 * Runs the given command in the operating system shell.
	 * 
	 * @param command the command to run, as if in a shell
	 * @param redirection the file into which the standard output of the command must be redirected
	 * @return the process into which the command is running
	 * @throws IOException if the command cannot be run
	 */
	private static Process run(String command, Optional<String> redirection) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder();

		if (System.getProperty("os.name").startsWith("Windows")) // Windows is different
			command = "cmd.exe /c " + command;

		processBuilder.command(command.split(" "));

        if (redirection.isPresent())
        	processBuilder.redirectOutput(new File(redirection.get()));

        return processBuilder.start();
	}

	/**
	 * Sends a {@code tx} request to the Tendermint process, to read the
	 * committed data about the Tendermint transaction with the given hash.
	 * 
	 * @param hash the hash of the Tendermint transaction to look for
	 * @return the response of Tendermint
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 */
	private String tx(String hash) throws IOException, TimeoutException, InterruptedException {
		String jsonTendermintRequest = "{\"method\": \"tx\", \"params\": {\"hash\": \"" +
			Base64.getEncoder().encodeToString(hexStringToByteArray(hash)) + "\", \"prove\": false }}";
	
		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Sends a {@code validators} request to the Tendermint process, to read the
	 * list of current validators of the Tendermint network.
	 * 
	 * @param page the page number
	 * @return number of entries per page (max 100)
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 */
	private String validators(int page, int perPage) throws IOException, TimeoutException, InterruptedException {
		String jsonTendermintRequest = "{\"method\": \"validators\", \"params\": {\"page\": " + page + ", \"per_page\": " + perPage + "}}";
		return postToTendermint(jsonTendermintRequest);
	}

	/*public String tx_search(String query) throws Exception {
		String jsonTendermintRequest = "{\"method\": \"tx_search\", \"params\": {\"query\": \"" +
			//Base64.getEncoder().encodeToString(
			query + "\", \"prove\": false, \"page\": \"1\", \"per_page\": \"30\", \"order_by\": \"asc\" }}";

		return postToTendermint(jsonTendermintRequest);
	}*/

	/*public String abci_query(String path, String data) throws Exception {
		String jsonTendermintRequest = "{\"method\": \"abci_query\", \"params\": {\"data\": \""
				+ bytesToHex(data.getBytes())
				//+ Base64.getEncoder().encodeToString(data.getBytes())
				+ "\", \"prove\": true }}";

		System.out.println(jsonTendermintRequest);
		return postToTendermint(jsonTendermintRequest);
	}*/

	/**
	 * Sends a {@code genesis} request to the Tendermint process, to read the
	 * genesis information, containing for instance the chain id of the node
	 * and the initial list of validators.
	 * 
	 * @return the response of Tendermint
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 */
	private String genesis() throws IOException, TimeoutException, InterruptedException {
		String jsonTendermintRequest = "{\"method\": \"genesis\"}";
		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Transforms a hexadecimal string into a byte array.
	 * 
	 * @param s the string
	 * @return the byte array
	 */
	private static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2)
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
	
	    return data;
	}

	/**
	 * Yields the URL of the Tendermint process.
	 * 
	 * @return the URL
	 * @throws MalformedURLException if the URL is not well formed
	 */
	private URL url() throws MalformedURLException {
		return new URL("http://127.0.0.1:" + node.config.tendermintPort);
	}

	/**
	 * Sends a POST request to the Tendermint process and yields the response.
	 * 
	 * @param jsonTendermintRequest the request to post, in JSON format
	 * @return the response
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing
	 */
	private String postToTendermint(String jsonTendermintRequest) throws IOException, TimeoutException, InterruptedException {
		HttpURLConnection connection = openPostConnectionToTendermint();
		writeInto(connection, jsonTendermintRequest);
		return readFrom(connection);
	}

	/**
	 * Reads the response from the given connection.
	 * 
	 * @param connection the connection
	 * @return the response
	 * @throws IOException if the response couldn't be read
	 */
	private static String readFrom(HttpURLConnection connection) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
			return br.lines().collect(Collectors.joining());
		}
	}

	/**
	 * Writes the given request into the given connection.
	 * 
	 * @param connection the connection
	 * @param jsonTendermintRequest the request
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing
	 */
	private void writeInto(HttpURLConnection connection, String jsonTendermintRequest) throws IOException, TimeoutException, InterruptedException {
		byte[] input = jsonTendermintRequest.getBytes("utf-8");

		for (int i = 0; i < node.config.maxPingAttempts; i++) {
			try (OutputStream os = connection.getOutputStream()) {
				os.write(input, 0, input.length);
				return;
			}
			catch (ConnectException e) {
				// not sure why this happens, randomly. It seems that the connection to the Tendermint process is flaky
				Thread.sleep(node.config.pingDelay);
			}
		}

		throw new TimeoutException("Cannot write into Tendermint's connection. Tried " + node.config.maxPingAttempts + " times");
	}

	/**
	 * Opens a http POST connection to the Tendermint process.
	 * 
	 * @return the connection
	 * @throws IOException if the connection cannot be opened
	 */
	private HttpURLConnection openPostConnectionToTendermint() throws IOException {
		HttpURLConnection con = (HttpURLConnection) url().openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);

		return con;
	}
}