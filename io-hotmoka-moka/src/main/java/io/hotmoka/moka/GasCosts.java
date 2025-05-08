/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.moka;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.internal.GasCostImpl;
import io.hotmoka.moka.internal.json.GasCostJson;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.websockets.beans.MappedDecoder;
import io.hotmoka.websockets.beans.MappedEncoder;
import jakarta.websocket.DecodeException;

/**
 * Providers of gas costs incurred for the already occurred execution of some requests.
 */
public abstract class GasCosts {

	private GasCosts() {}

	// TODO: maybe remove these providers?
	// TODO: maybe remove varargs?
	/**
	 * Creates the gas cost incurred for the already occurred execution of a set of requests.
	 * 
	 * @param node the node that executed the requests
	 * @param gasPrice the gas price used for the requests
	 * @param requests the requests whose gas cost must be computed
	 * @return the gas cost
	 * @throws InterruptedException if the execution gets interrupted
	 * @throws TimeoutException if no answer arrives within the expected time window
	 * @throws UnknownReferenceException if some request cannot be found in the store of the node
	 * @throws NodeException if the node is not able to complete the operation correctly
	 * @throws NoSuchAlgorithmException if hashing algorithm for the transaction requests is not available
	 */
	public static GasCost of(Node node, BigInteger gasPrice, TransactionRequest<?>... requests) throws NodeException, TimeoutException, InterruptedException, UnknownReferenceException, NoSuchAlgorithmException {
		return new GasCostImpl(node, gasPrice, requests);
	}

	/**
	 * Creates the gas cost incurred for the already occurred execution of a set of requests, identified by their references.
	 * 
	 * @param node the node that executed the requests
	 * @param gasPrice the gas price used for the requests
	 * @param references the references of the requests whose consumed gas must be computed
	 * @return the gas cost
	 * @throws InterruptedException if the execution gets interrupted
	 * @throws TimeoutException if no answer arrives within the expected time window
	 * @throws UnknownReferenceException if some request transaction cannot be found in the store of the node
	 * @throws NodeException if the node is not able to complete the operation correctly
	 */
	public static GasCost of(Node node, BigInteger gasPrice, TransactionReference... references) throws NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		return new GasCostImpl(node, gasPrice, references);
	}

	/**
	 * Yields the gas cost from its JSON representation.
	 * 
	 * @param json the JSON representation of the gas cost
	 * @return the output of the command
	 * @throws DecodeException if {@code json} cannot be decoded into the output
	 */
	public static GasCost from(String json) throws DecodeException {
		return new Decoder().decode(json);
	}

	/**
	 * JSON representation.
	 */
	public static class Json extends GasCostJson {
	
		/**
		 * Creates the JSON representation for the given gas cost.
		 * 
		 * @param output the gas cost
		 */
		public Json(GasCost output) {
			super(output);
		}
	}

	/**
	 * JSON encoder.
	 */
	public static class Encoder extends MappedEncoder<GasCost, Json> {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {
			super(Json::new);
		}
	}

	/**
	 * JSON decoder.
	 */
	public static class Decoder extends MappedDecoder<GasCost, Json> {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {
			super(Json.class);
		}
	}
}