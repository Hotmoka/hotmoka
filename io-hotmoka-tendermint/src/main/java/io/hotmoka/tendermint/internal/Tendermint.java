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
import java.net.URL;
import java.util.Base64;

import com.google.gson.Gson;

import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.tendermint.internal.beans.TendermintTopLevelResult;
import io.hotmoka.tendermint.internal.beans.TendermintTxResponse;

/**
 * A proxy object that connects to the Tendermint process, sends transactions to it
 * and gets responses from it.
 */
class Tendermint {

	/**
	 * The maximal number of connection attempts to the Tendermint URL during ping,
	 */
	private final static int MAX_CONNECTION_ATTEMPTS = 10;

	/**
	 * The maximal number of polling attempts, while waiting for the result of a posted transaction.
	 */
	private final static int MAX_POLLING_ATTEMPTS = 100;

	/**
	 * The delay between the first two polling attempts, while waiting for the result of a posted transaction.
	 * This delay is then increased by 30% at each subsequent attempt.
	 */
	private final static int POLLING_DELAY = 100;

	/**
	 * The URL of the Tendermint process. For instance: {@code http://localhost:26657}.
	 */
	private final URL url;

	/**
	 * An object for JSON manipulation.
	 */
	private final Gson gson = new Gson();

	/**
	 * Creates a proxy to a Tendermint process.
	 * 
	 * @param url the URL of the Tendermint process. For instance: {@code http://localhost:26657}
	 */
	Tendermint(URL url) {
		this.url = url;
	}

	void ping() throws IOException {
		for (int reconnections = 1; reconnections <= MAX_CONNECTION_ATTEMPTS; reconnections++) {
			try {
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.connect();
				return;
			}
			catch (ConnectException e) {
				System.out.println("Error while connecting to Tendermint process at " + url + ": " + e.getMessage());
				System.out.println("I will try to reconnect in 10 seconds... (" + reconnections + "/10)");

				try {
					Thread.sleep(10000);
				}
				catch (InterruptedException e2) {}
			}
		}

		throw new IOException("Cannot connect to Tendermint process at " + url + ". Tried " + MAX_CONNECTION_ATTEMPTS + " times");
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
	 * Transforms a hexadecimal string into a byte array.
	 * 
	 * @param s the string
	 * @return the byte array
	 */
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2)
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));

	    return data;
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
		try (OutputStream os = connection.getOutputStream()) {
		    byte[] input = jsonTendermintRequest.getBytes("utf-8");
		    os.write(input, 0, input.length);
		}
	}

	/**
	 * Opens a http POST connection to the Tendermint process.
	 * 
	 * @return the connection
	 * @throws IOException if the connection cannot be opened
	 */
	private HttpURLConnection openPostConnectionToTendermint() throws IOException {
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
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