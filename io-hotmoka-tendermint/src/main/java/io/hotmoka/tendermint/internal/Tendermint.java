package io.hotmoka.tendermint.internal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import io.hotmoka.beans.requests.TransactionRequest;

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
	 * The URL of the Tendermint process. For instance: {@code http://localhost:26657}.
	 */
	private final URL url;

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
	 * a {@code broadcast_tx_commit} transaction.
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
	 * Sends posts the given request to the Tendermint process and yields the response.
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
	 * Opens a http connection to the Tendermint process.
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
	 * Serializes the given request and Base64-encodes its serialization into a string.
	 * 
	 * @param request the request
	 * @return the Base64-encoded serialization of {@code request}
	 * @throws IOException if serialization fails
	 */
	private static String base64EncodedSerializationOf(TransactionRequest<?> request) throws IOException {
		String base64EncodedHotmokaRequest;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(request);
			base64EncodedHotmokaRequest = Base64.getEncoder().encodeToString(baos.toByteArray());
		}
		return base64EncodedHotmokaRequest;
	}
}