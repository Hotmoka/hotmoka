package io.hotmoka.tendermint.internal;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.tendermint.internal.beans.TendermintBroadcastTxResponse;
import io.hotmoka.tendermint.internal.beans.TendermintTopLevelResult;
import io.hotmoka.tendermint.internal.beans.TendermintTxResponse;
import io.hotmoka.tendermint.internal.beans.TendermintTxResult;
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

	/**
	 * Spawns the Tendermint process and creates a proxy to it. It assumes that
	 * the {@code tendermint} command can be executed from the command path.
	 * 
	 * @param config the configuration of the blockchain
	 * @param reset true if and only if the blockchain must be initialized from scratch; in that case,
	 *              the directory of the blockchain gets deleted
	 * @throws Exception if the Tendermint process cannot be spawned
	 */
	Tendermint(TendermintBlockchainImpl node, boolean reset) throws Exception {
		this.node = node;

		if (reset)
			if (run("tendermint init --home " + node.getConfig().dir + "/blocks").waitFor() != 0)
				throw new IOException("Tendermint initialization failed");

		// spawns a process that remains in background
		this.process = run("tendermint node --home " + node.getConfig().dir + "/blocks --abci grpc --proxy_app tcp://127.0.0.1:" + node.getConfig().abciPort);
		// wait until it is up and running
		ping();
	}

	@Override
	public void close() throws InterruptedException, UnsupportedEncodingException, IOException {
		
			process.children().forEach(ProcessHandle::destroy);
			process.destroy();
			process.waitFor();
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream(), "utf-8"))) {
				System.out.println(br.lines().collect(Collectors.joining()));
			}

	}

	/**
	 * Sends the given {@code request} to the Tendermint process, inside a {@code broadcast_tx_async} Tendermint request.
	 * 
	 * @param request the request to send
	 * @return the response of Tendermint
	 * @throws Exception if the connection couldn't be opened or the request could not be sent
	 */
	String broadcastTxAsync(TransactionRequest<?> request) throws Exception {
		String jsonTendermintRequest = "{\"method\": \"broadcast_tx_async\", \"params\": {\"tx\": \"" +  Base64.getEncoder().encodeToString(request.toByteArray()) + "\"}}";
		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Yields the Hotmoka transaction reference specified in the Tendermint result for the Tendermint
	 * transaction with the given hash.
	 * 
	 * @param hash the hash of the transaction to look for
	 * @return the Hotmoka transaction reference
	 * @throws Exception if the connection couldn't be opened or the request could not be sent or the result was incorrect
	 */
	Optional<TransactionReference> getTransactionReferenceFor(String hash) throws Exception {
		TendermintTxResponse response = gson.fromJson(tx(hash), TendermintTxResponse.class);
		if (response.error == null) {
			TendermintTxResult tx_result = response.result.tx_result;

			if (tx_result == null)
				throw new IllegalStateException("no result for transaction " + hash);

			String data = tx_result.data;
			if (data == null)
				throw new IllegalStateException(tx_result.info);

			Object dataAsObject;
			try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)))) {
				dataAsObject = TransactionReference.from(ois);
			}

			if (!(dataAsObject instanceof TransactionReference))
				throw new IllegalStateException("no Hotmoka transaction reference found in data field of Tendermint transaction");

			return Optional.of((TransactionReference) dataAsObject);
		}

		return Optional.empty();
	}

	/**
	 * Extracts the Tendermint hash associated to a transaction whose response is given.
	 * 
	 * @param response the Tendermint response
	 * @return the hash
	 */
	String extractHashFromBroadcastTxResponse(String response) {
		TendermintBroadcastTxResponse parsedResponse = gson.fromJson(response, TendermintBroadcastTxResponse.class);
	
		TxError error = parsedResponse.error;
		if (error != null)
			throw new IllegalStateException("Tendermint transaction failed: " + error.message + ": " + error.data);
	
		TendermintTopLevelResult result = parsedResponse.result;
	
		if (result == null)
			throw new IllegalStateException("missing result in Tendermint response");
	
		String hash = result.hash;
		if (hash == null)
			throw new IllegalStateException("missing hash in Tendermint response");
	
		return hash;
	}

	/**
	 * Runs the given command in operating system shell.
	 * 
	 * @param command the command to run, as if in a shell
	 * @return the process into which the command is running
	 * @throws IOException if the command cannot be run
	 */
	private static Process run(String command) throws IOException {
		if (System.getProperty("os.name").startsWith("Windows")) // Windows is different
			command = "cmd.exe /c " + command;
	
		return Runtime.getRuntime().exec(command);
	}

	/**
	 * Sends a {@code tx} request to the Tendermint process, to read the
	 * committed data about the Tendermint transaction with the given hash.
	 * 
	 * @param hash the hash of the Tendermint transaction to look for
	 * @return the response of Tendermint
	 * @throws Exception if the connection couldn't be opened or the request could not be sent
	 */
	private String tx(String hash) throws Exception {
		String jsonTendermintRequest = "{\"method\": \"tx\", \"params\": {\"hash\": \"" +
			Base64.getEncoder().encodeToString(hexStringToByteArray(hash)) + "\", \"prove\": false }}";
	
		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Waits until the Tendermint process responds to ping.
	 * 
	 * @throws IOException if it is not possible to connect to the Tendermint process
	 * @throws TimeoutException if tried many times, but never got a reply
	 * @throws InterruptedException if interrupted while pinging
	 */
	private void ping() throws TimeoutException, InterruptedException, IOException {
		for (int reconnections = 1; reconnections <= node.getConfig().maxPingAttempts; reconnections++) {
			try {
				HttpURLConnection connection = openPostConnectionToTendermint();
				try (OutputStream os = connection.getOutputStream()) {
					return;
				}
			}
			catch (ConnectException e) {
				// take a nap, then try again
				Thread.sleep(node.getConfig().pingDelay);
			}
		}
	
		throw new TimeoutException("Cannot connect to Tendermint process at " + url() + ". Tried " + node.getConfig().maxPingAttempts + " times");
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
		return new URL("http://127.0.0.1:" + node.getConfig().tendermintPort);
	}

	/**
	 * Sends a POST request to the Tendermint process and yields the response.
	 * 
	 * @param jsonTendermintRequest the request to post, in JSON format
	 * @return the response
	 * @throws Exception if the request couldn't be sent
	 */
	private String postToTendermint(String jsonTendermintRequest) throws Exception {
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
	 * @throws Exception if the request cannot be written
	 */
	private void writeInto(HttpURLConnection connection, String jsonTendermintRequest) throws Exception {
		byte[] input = jsonTendermintRequest.getBytes("utf-8");

		for (int i = 0; i < node.getConfig().maxPingAttempts; i++) {
			try (OutputStream os = connection.getOutputStream()) {
				os.write(input, 0, input.length);
				return;
			}
			catch (ConnectException e) {
				// not sure why this happens, randomly. It seems that the connection to the Tendermint process is flaky
				Thread.sleep(node.getConfig().pingDelay);
			}
		}

		throw new TimeoutException("Cannot write into Tendermint's connection. Tried " + node.getConfig().maxPingAttempts + " times");
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