/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

package io.hotmoka.node.service.api;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.api.requests.InitializationTransactionRequest;
import io.hotmoka.beans.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.api.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.Node;

/**
 * A network service that exposes a REST API to a Hotmoka node.
 */
@ThreadSafe
public interface NodeService extends AutoCloseable {

	/**
	 * The network endpoint path where {@link Node#getNodeInfo()} is published.
	 */
	String GET_NODE_INFO_ENDPOINT = "/get_node_info";

	/**
	 * The network endpoint path where {@link Node#getConsensusConfig()} is published.
	 */
	String GET_CONSENSUS_CONFIG_ENDPOINT = "/get_consensus_config";

	/**
	 * The network endpoint path where {@link Node#getTakamakaCode()} is published.
	 */
	String GET_TAKAMAKA_CODE_ENDPOINT = "/get_takamaka_code";

	/**
	 * The network endpoint path where {@link Node#getManifest()} is published.
	 */
	String GET_MANIFEST_ENDPOINT = "/get_manifest";

	/**
	 * The network endpoint path where {@link Node#getClassTag(StorageReference)} is published.
	 */
	String GET_CLASS_TAG_ENDPOINT = "/get_class_tag";

	/**
	 * The network endpoint path where {@link Node#getState(StorageReference)} is published.
	 */
	String GET_STATE_ENDPOINT = "/get_state";

	/**
	 * The network endpoint path where {@link Node#getRequest(TransactionReference)} is published.
	 */
	String GET_REQUEST_ENDPOINT = "/get_request";

	/**
	 * The network endpoint path where {@link Node#getResponse(TransactionReference)} is published.
	 */
	String GET_RESPONSE_ENDPOINT = "/get_response";

	/**
	 * The network endpoint path where {@link Node#getPolledResponse(TransactionReference)} is published.
	 */
	String GET_POLLED_RESPONSE_ENDPOINT = "/get_polled_response";

	/**
	 * The network endpoint path where {@link Node#runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest)} is published.
	 */
	String RUN_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT = "/run_instance_method_call_transaction_request";

	/**
	 * The network endpoint path where {@link Node#runStaticMethodCallTransaction(StaticMethodCallTransactionRequest)} is published.
	 */
	String RUN_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT = "/run_static_method_call_transaction_request";

	/**
	 * The network endpoint path where {@link Node#addJarStoreTransaction(JarStoreTransactionRequest)} is published.
	 */
	String ADD_JAR_STORE_TRANSACTION_ENDPOINT = "/add_jar_store_transaction_request";

	/**
	 * The network endpoint path where {@link Node#addGameteCreationTransaction(GameteCreationTransactionRequest)} is published.
	 */
	String ADD_GAMETE_CREATION_TRANSACTION_ENDPOINT = "/add_gamete_creation_transaction_request";

	/**
	 * The network endpoint path where {@link Node#addJarStoreInitialTransaction(JarStoreInitialTransactionRequest)} is published.
	 */
	String ADD_JAR_STORE_INITIAL_TRANSACTION_ENDPOINT = "/add_jar_store_initial_transaction_request";

	/**
	 * The network endpoint path where {@link Node#addInitializationTransaction(InitializationTransactionRequest)} is published.
	 */
	String ADD_INITIALIZATION_TRANSACTION_ENDPOINT = "/add_initialization_transaction_request";

	/**
	 * The network endpoint path where {@link Node#addConstructorCallTransaction(ConstructorCallTransactionRequest)} is published.
	 */
	String ADD_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT = "/add_constructor_call_transaction_request";

	/**
	 * The network endpoint path where {@link Node#addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest)} is published.
	 */
	String ADD_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT = "/add_instance_method_call_transaction_request";

	/**
	 * The network endpoint path where {@link Node#addStaticMethodCallTransaction(StaticMethodCallTransactionRequest)} is published.
	 */
	String ADD_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT = "/add_static_method_call_transaction_request";

	/**
	 * The network endpoint path where {@link Node#postConstructorCallTransaction(ConstructorCallTransactionRequest)} is published.
	 */
	String POST_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT = "/post_constructor_call_transaction_request";

	/**
	 * The network endpoint path where {@link Node#postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest)} is published.
	 */
	String POST_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT = "/post_instance_method_call_transaction_request";

	/**
	 * The network endpoint path where {@link Node#postStaticMethodCallTransaction(StaticMethodCallTransactionRequest)} is published.
	 */
	String POST_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT = "/post_static_method_call_transaction_request";

	/**
	 * The network endpoint path where {@link Node#postJarStoreTransaction(JarStoreTransactionRequest)} is published.
	 */
	String POST_JAR_STORE_TRANSACTION_ENDPOINT = "/post_jar_store_transaction_request";

	/**
	 * The network endpoint path where the events are published.
	 */
	String EVENTS_ENDPOINT = "/events";

	/**
	 * Stops the service and releases its resources.
	 */
	@Override
	void close();
}