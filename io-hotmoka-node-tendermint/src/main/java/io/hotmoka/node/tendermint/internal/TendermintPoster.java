/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.tendermint.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.hotmoka.crypto.Base64;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.node.tendermint.internal.beans.TendermintBroadcastTxResponse;
import io.hotmoka.node.tendermint.internal.beans.TendermintGenesisResponse;
import io.hotmoka.node.tendermint.internal.beans.TendermintStatusResponse;
import io.hotmoka.node.tendermint.internal.beans.TendermintValidatorPriority;
import io.hotmoka.node.tendermint.internal.beans.TendermintValidatorsResponse;
import io.hotmoka.node.tendermint.internal.beans.TxError;

/**
 * An object that posts requests to a Tendermint process.
 */
public class TendermintPoster {
	private final static Logger LOGGER = Logger.getLogger(TendermintPoster.class.getName());

	private final TendermintNodeConfig config;

	/**
	 * The port of the Tendermint process on localhost.
	 */
	private final int tendermintPort;

	/**
	 * An object for JSON manipulation.
	 */
	private final Gson gson = new Gson();

	private final AtomicInteger nextId = new AtomicInteger();

	TendermintPoster(TendermintNodeConfig config, int tendermintPort) {
		this.config = config;
		this.tendermintPort = tendermintPort;
	}

	/**
	 * Sends the given {@code request} to the Tendermint process, inside a {@code broadcast_tx_async} Tendermint request.
	 * 
	 * @param request the request to send
	 * @throws NodeException if the node is misbehaving
	 * @throws TimeoutException if the operation could not be completed on time
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 */
	void postRequest(TransactionRequest<?> request) throws NodeException, InterruptedException, TimeoutException {
		String jsonTendermintRequest = "{\"method\": \"broadcast_tx_async\", \"params\": {\"tx\": \"" + Base64.toBase64String(request.toByteArray()) + "\"}, \"id\": " + nextId.getAndIncrement() + "}";
		TendermintBroadcastTxResponse response;

		try {
			response = gson.fromJson(postToTendermint(jsonTendermintRequest), TendermintBroadcastTxResponse.class);
		}
		catch (IOException | JsonSyntaxException e) {
			LOGGER.log(Level.WARNING, "the Tendermint engine did not provide information about a request", e);
			throw new NodeException("The Tendermint engine did not provide information about a request", e);
		}

		if (response == null) {
			LOGGER.severe("empty response about a request, from the underlying Tendermint engine");
			throw new NodeException("Empty response about a request, from the underlying Tendermint engine");
		}

		TxError error = response.error;
		if (error != null)
			throw new NodeException("Tendermint transaction failed: " + error.message + ": " + error.data);
	}

	/**
	 * Yields the chain id of the underlying Tendermint engine.
	 * 
	 * @return the chain id
	 * @throws NodeException if the node is misbehaving
	 * @throws TimeoutException if the operation could not be completed on time
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 */
	String getTendermintChainId() throws NodeException, TimeoutException, InterruptedException {
		TendermintGenesisResponse response;

		try {
			response = gson.fromJson(genesis(), TendermintGenesisResponse.class);
		}
		catch (IOException | JsonSyntaxException e) {
			LOGGER.log(Level.SEVERE, "the Tendermint engine did not answer a request about its chain id", e);
			throw new NodeException("The Tendermint engine did not answer a request about its chain id", e);
		}

		if (response == null) {
			LOGGER.severe("no chain id in Tendermint response");
			throw new NodeException("No chain id in Tendermint response");
		}

		if (response.error != null)
			throw new NodeException(response.error);

		String chainId;
		if (response.result == null || response.result.genesis == null || (chainId = response.result.genesis.chain_id) == null) {
			LOGGER.severe("no chain id in Tendermint response");
			throw new NodeException("No chain id in Tendermint response");
		}

		return chainId;
	}

	/**
	 * Yields the genesis time of the underlying Tendermint engine, in UTC pattern.
	 * 
	 * @return the genesis time, in UTC pattern
	 * @throws NodeException if the node is misbehaving
	 * @throws TimeoutException if the operation could not be completed on time
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 */
	String getGenesisTime() throws NodeException, TimeoutException, InterruptedException {
		TendermintGenesisResponse response;

		try {
			response = gson.fromJson(genesis(), TendermintGenesisResponse.class);
		}
		catch (IOException | JsonSyntaxException e) {
			LOGGER.log(Level.WARNING, "the Tendermint engine did not answer a request about its genesis time", e);
			throw new NodeException("The Tendermint engine did not answer a request about its genesis time", e);
		}

		if (response == null) {
			LOGGER.severe("no genesis time in Tendermint response");
			throw new NodeException("No genesis time in Tendermint response");
		}

		if (response.error != null)
			throw new NodeException(response.error);

		String genesisTime;
		if (response.result == null || response.result.genesis == null || (genesisTime = response.result.genesis.genesis_time) == null) {
			LOGGER.severe("no genesis time in Tendermint response");
			throw new NodeException("No genesis time in Tendermint response");
		}

		return genesisTime;
	}

	/**
	 * Yields the Tendermint identifier of the node. This is the hexadecimal
	 * hash of the public key of the node and is used to identify the node as a peer in the network.
	 * 
	 * @return the hexadecimal ID of the node
	 * @throws NodeException if the node is misbehaving
	 * @throws TimeoutException if the operation could not be completed on time
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 */
	String getNodeID() throws NodeException, TimeoutException, InterruptedException {
		TendermintStatusResponse response;

		try {
			response = gson.fromJson(status(), TendermintStatusResponse.class);
		}
		catch (IOException | JsonSyntaxException e) {
			LOGGER.log(Level.WARNING, "the Tendermint engine did not answer a request about its identifier", e);
			throw new NodeException("The Tendermint engine did not answer a request about its identifier", e);
		}

		if (response == null) {
			LOGGER.severe("no node identifier in Tendermint response");
			throw new NodeException("No node identifier in Tendermint response");
		}

		if (response.error != null)
			throw new NodeException(response.error);

		String id;
		if (response.result == null || response.result.node_info == null || (id = response.result.node_info.id) == null) {
			LOGGER.severe("no node identifier in Tendermint response");
			throw new NodeException("No node identifier in Tendermint response");
		}

		return id;
	}

	/**
	 * Yields the information about the validators of the underlying Tendermint engine.
	 * 
	 * @return the information
	 * @throws NodeException if the node is misbehaving
	 * @throws TimeoutException if the operation could not be completed on time
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 */
	TendermintValidator[] getTendermintValidators() throws NodeException, TimeoutException, InterruptedException {
		TendermintValidatorsResponse response;

		try {
			response = gson.fromJson(validators(1, 100), TendermintValidatorsResponse.class);
		}
		catch (IOException | JsonSyntaxException e) {
			LOGGER.log(Level.WARNING, "the Tendermint engine did not provide information about its validators", e);
			throw new NodeException("The Tendermint engine did not provide information about its validators", e);
		}

		if (response == null) {
			LOGGER.severe("no validators in Tendermint response");
			throw new NodeException("No validators in Tendermint response");
		}

		if (response.error != null)
			throw new NodeException(response.error);

		List<TendermintValidatorPriority> validators;
		if (response.result == null || (validators = response.result.validators) == null) {
			LOGGER.severe("no validators in Tendermint response");
			throw new NodeException("No validators in Tendermint response");
		}

		TendermintValidator[] result = new TendermintValidator[validators.size()];
		int pos = 0;
		for (var validator: validators)
			result[pos++] = intoTendermintValidator(validator);

		return result;
	}

	/**
	 * Opens a http POST connection to the underlying Tendermint engine.
	 * 
	 * @return the connection
	 * @throws IOException if the connection cannot be opened
	 */
	HttpURLConnection openPostConnectionToTendermint() throws IOException {
		var con = (HttpURLConnection) url().openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; UTF-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);
	
		return con;
	}

	/**
	 * Yields the URL of the Tendermint process.
	 * 
	 * @return the URL
	 * @throws URISyntaxException if the URL is not well formed
	 */
	URL url() throws MalformedURLException {
		return URI.create("http://127.0.0.1:" + tendermintPort).toURL();
	}

	private static TendermintValidator intoTendermintValidator(TendermintValidatorPriority validatorPriority) throws NodeException {
		if (validatorPriority.pub_key == null)
			throw new NodeException("null pub_key in Tendermint validator information");
		else
			return new TendermintValidator(validatorPriority.address, validatorPriority.voting_power, validatorPriority.pub_key.value, validatorPriority.pub_key.type, NodeException::new);
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
		String jsonTendermintRequest = "{\"method\": \"validators\", \"params\": {\"page\": \"" + page + "\", \"per_page\": \"" + perPage + "\"}, \"id\": " + nextId.getAndIncrement() + "}";
		return postToTendermint(jsonTendermintRequest);
	}

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
		return postToTendermint("{\"method\": \"genesis\", \"id\": " + nextId.getAndIncrement() + "}");
	}

	/**
	 * Sends a {@code status} request to the Tendermint process, to read the
	 * node status information, containing for instance the node id.
	 * 
	 * @return the response of Tendermint
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 */
	private String status() throws IOException, TimeoutException, InterruptedException {
		return postToTendermint("{\"method\": \"status\", \"id\": " + nextId.getAndIncrement() + "}");
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
		try (var br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
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
		byte[] input = jsonTendermintRequest.getBytes(StandardCharsets.UTF_8);

		for (long i = 0; i < config.getMaxPingAttempts(); i++) {
			try (var os = connection.getOutputStream()) {
				os.write(input, 0, input.length);
				return;
			}
			catch (ConnectException e) {
				System.out.println("!!!!!!!!!!!!!!!!!");
				// not sure why this happens, randomly. It seems that the connection to the Tendermint process is flaky
				Thread.sleep(config.getPingDelay());
			}
		}

		throw new TimeoutException("Cannot write into Tendermint's connection. Tried " + config.getMaxPingAttempts() + " times");
	}
}