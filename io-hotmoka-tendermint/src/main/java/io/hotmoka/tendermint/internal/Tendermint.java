package io.hotmoka.tendermint.internal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

import com.google.gson.Gson;

import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.internal.beans.TendermintTopLevelResult;
import io.hotmoka.tendermint.internal.beans.TendermintTxResponse;

/**
 * A proxy object that connects to the Tendermint process, sends transactions to it
 * and gets responses from it.
 */
class Tendermint implements AutoCloseable {

	/**
	 * The maximal number of connection attempts to the Tendermint process during ping,
	 */
	private final static int MAX_PING_ATTEMPTS = 20;

	/**
	 * The delay of the first ping attempt of the Tendermint process.
	 * This delay is then increased by 30% at each subsequent attempt.
	 */
	private final static int PING_DELAY = 200;

	/**
	 * The maximal number of polling attempts, while waiting for the result of a posted transaction.
	 */
	private final static int MAX_POLLING_ATTEMPTS = 100;

	/**
	 * The delay of two subsequent polling attempts, while waiting for the result of a posted transaction.
	 * This delay is then increased by 30% at each subsequent attempt.
	 */
	private final static int POLLING_DELAY = 10;

	/**
	 * The configuration of the blockchain.
	 */
	private final Config config;

	/**
	 * An object for JSON manipulation.
	 */
	private final Gson gson = new Gson();

	/**
	 * The Tendermint process;
	 */
	private final Process process;

	/**
	 * Spawns the Tendermint process and creates a proxy to it.
	 * It assumes that the {@code tendermint} command can be executed
	 * from the command path.
	 * 
	 * @param config the configuration of the blockchain
	 * @param reset true if and only if the blockchain must be initialized
	 * @throws IOException if the Tendermint process cannot be spawned
	 * @throws InterruptedException if the Tendermint process is interrupted while resetting its state
	 */
	Tendermint(Config config, boolean reset) throws IOException, InterruptedException {
		this.config = config;

		if (reset)
			if (run("tendermint init --home " + config.dir + "/blocks").waitFor() != 0)
				throw new IOException("Tendermint initialization failed");

		this.process = run("tendermint node --home " + config.dir + "/blocks --abci grpc --proxy_app tcp://127.0.0.1:" + config.abciPort); // process remains in background

		ping();
	}

	/**
	 * Runs the given command.
	 * 
	 * @param command the command to run, as if in a shell
	 * @return the process into which the command is running
	 * @throws IOException if the command cannot be run
	 */
	private static Process run(String command) throws IOException {
		if (System.getProperty("os.name").startsWith("Windows"))
			command = "cmd.exe /c " + command;

		return Runtime.getRuntime().exec(command);
	}

	@Override
	public void close() throws InterruptedException {
		process.destroy();
		process.waitFor();
	}

	/**
	 * Sends the given {@code request} to the Tendermint process, inside
	 * a {@code broadcast_tx_commit} Tendermint request.
	 * 
	 * @param request the request to send
	 * @return the response of Tendermint
	 * @throws IOException if the connection couldn't be opened or the request could not be sent
	 */
	String broadcastTxCommit(TransactionRequest<?> request) throws IOException {
		String base64EncodedHotmokaRequest = base64EncodedSerializationOf(request);
		String jsonTendermintRequest = "{\"method\": \"broadcast_tx_commit\", \"params\": {\"tx\": \"" + base64EncodedHotmokaRequest + "\"}}";

		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Sends the given {@code request} to the Tendermint process, inside
	 * a {@code broadcast_tx_sync} Tendermint request.
	 * 
	 * @param request the request to send
	 * @return the response of Tendermint
	 * @throws IOException if the connection couldn't be opened or the request could not be sent
	 */
	String broadcastTxSync(TransactionRequest<?> request) throws IOException {
		String base64EncodedHotmokaRequest = base64EncodedSerializationOf(request);
		String jsonTendermintRequest = "{\"method\": \"broadcast_tx_sync\", \"params\": {\"tx\": \"" + base64EncodedHotmokaRequest + "\"}}";

		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Sends the given {@code request} to the Tendermint process, inside
	 * a {@code broadcast_tx_async} Tendermint request.
	 * 
	 * @param request the request to send
	 * @return the response of Tendermint
	 * @throws IOException if the connection couldn't be opened or the request could not be sent
	 */
	String broadcastTxAsync(TransactionRequest<?> request) throws IOException {
		String base64EncodedHotmokaRequest = base64EncodedSerializationOf(request);
		String jsonTendermintRequest = "{\"method\": \"broadcast_tx_async\", \"params\": {\"tx\": \"" + base64EncodedHotmokaRequest + "\"}}";

		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Sends a {@code tx} request to the Tendermint process, to read the
	 * committed data about the Tendermint transaction with the given hash.
	 * 
	 * @param hash the hash of the Tendermint transaction to look for
	 * @return the response of Tendermint
	 * @throws IOException if the connection couldn't be opened or the request could not be sent
	 */
	String tx(String hash) throws IOException {
		String jsonTendermintRequest = "{\"method\": \"tx\", \"params\": {\"hash\": \"" +
			Base64.getEncoder().encodeToString(hexStringToByteArray(hash)) + "\", \"prove\": false }}";

		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Pools Tendermint until a transaction with the given hash has been successfully committed.
	 * 
	 * @param hash the hash of the transaction to look for
	 * @return the result of the transaction
	 * @throws IOException if the connection couldn't be opened or the request could not be sent
	 */
	TendermintTopLevelResult poll(String hash) throws IOException {
		int delay = POLLING_DELAY;

		for (int i = 0; i < MAX_POLLING_ATTEMPTS; i++) {
			TendermintTxResponse response = gson.fromJson(tx(hash), TendermintTxResponse.class);
			if (response.error == null)
				return response.result;

			try {
				Thread.sleep(delay);
			}
			catch (InterruptedException e) {
			}

			// we increase the delay, for next attempt
			delay = (130 * delay) / 100;
		}

		throw new IOException("cannot find transaction " + hash);
	}

	/**
	 * Waits until the Tendermint process responds to ping.
	 * It gives up after {@MAX_CONNECTION_ATTEMPTS}, with an exception.
	 * 
	 * @throws IOException if it is not possible to connect to the Tendermint process
	 */
	private void ping() throws IOException {
		int delay = PING_DELAY;
	
		for (int reconnections = 1; reconnections <= MAX_PING_ATTEMPTS; reconnections++) {
			try {
				Thread.sleep(delay);
			}
			catch (InterruptedException e2) {}
	
			try {
				HttpURLConnection connection = openPostConnectionToTendermint();
				try (OutputStream os = connection.getOutputStream()) {
				}

				return;
			}
			catch (ConnectException e) {
				// we increase the delay, for next attempt
				delay = (130 * delay) / 100;
			}
		}
	
		throw new IOException("Cannot connect to Tendermint process at " + url() + ". Tried " + MAX_PING_ATTEMPTS + " times");
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
		return new URL("http://127.0.0.1:" + config.tendermintPort);
	}

	/**
	 * Sends a POST request to the Tendermint process and yields the response.
	 * 
	 * @param jsonTendermintRequest the request to post, in JSON format
	 * @return the response
	 * @throws IOException if the request couldn't be sent
	 */
	private String postToTendermint(String jsonTendermintRequest) throws IOException {
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
			StringBuilder response = new StringBuilder();
			String responseLine;
			while ((responseLine = br.readLine()) != null)
				response.append(responseLine.trim());

			return response.toString();
		}
	}

	/**
	 * Writes the given request into the given connection.
	 * 
	 * @param connection the connection
	 * @param jsonTendermintRequest the request
	 * @throws IOException if the request cannot be written
	 */
	private static void writeInto(HttpURLConnection connection, String jsonTendermintRequest) throws IOException {
		int delay = PING_DELAY;
		for (int i = 0; i < MAX_PING_ATTEMPTS; i++) {
			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = jsonTendermintRequest.getBytes("utf-8");
				os.write(input, 0, input.length);
				return;
			}
			catch (ConnectException e) {
				// not sure why this happens, randomly. It seems that the connection
				// to the Tendermint process is flaky
				try {
					Thread.sleep(delay);
				}
				catch (InterruptedException e1) {
				}

				delay = delay * 130 / 100;
			}
		}

		throw new IOException("Cannot write into Tendermint's connection. Tried " + MAX_PING_ATTEMPTS + " times");
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

	/**
	 * Serializes the given object and Base64-encodes its serialization into a string.
	 * 
	 * @param object the object
	 * @return the Base64-encoded serialization of {@code object}
	 * @throws IOException if serialization fails
	 */
	private static String base64EncodedSerializationOf(Serializable object) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(object);
			return Base64.getEncoder().encodeToString(baos.toByteArray());
		}
	}
}