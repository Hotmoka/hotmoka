/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.remote.internal;

import static io.hotmoka.node.service.api.NodeService.ADD_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.ADD_GAMETE_CREATION_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.ADD_INITIALIZATION_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.ADD_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.ADD_JAR_STORE_INITIAL_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.ADD_JAR_STORE_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.ADD_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.EVENTS_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_CLASS_TAG_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_CONFIG_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_INDEX_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_INFO_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_MANIFEST_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_POLLED_RESPONSE_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_REQUEST_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_RESPONSE_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_STATE_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_TAKAMAKA_CODE_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.POST_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.POST_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.POST_JAR_STORE_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.POST_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.RUN_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.RUN_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.CodeFutures;
import io.hotmoka.node.JarFutures;
import io.hotmoka.node.SubscriptionsManagers;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.ConstructorFuture;
import io.hotmoka.node.api.JarFuture;
import io.hotmoka.node.api.MethodFuture;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.SubscriptionsManager;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.messages.AddConstructorCallTransactionMessages;
import io.hotmoka.node.messages.AddConstructorCallTransactionResultMessages;
import io.hotmoka.node.messages.AddGameteCreationTransactionMessages;
import io.hotmoka.node.messages.AddGameteCreationTransactionResultMessages;
import io.hotmoka.node.messages.AddInitializationTransactionMessages;
import io.hotmoka.node.messages.AddInitializationTransactionResultMessages;
import io.hotmoka.node.messages.AddInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.AddInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.AddJarStoreInitialTransactionMessages;
import io.hotmoka.node.messages.AddJarStoreInitialTransactionResultMessages;
import io.hotmoka.node.messages.AddJarStoreTransactionMessages;
import io.hotmoka.node.messages.AddJarStoreTransactionResultMessages;
import io.hotmoka.node.messages.AddStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.AddStaticMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.EventMessages;
import io.hotmoka.node.messages.GetClassTagMessages;
import io.hotmoka.node.messages.GetClassTagResultMessages;
import io.hotmoka.node.messages.GetConfigMessages;
import io.hotmoka.node.messages.GetConfigResultMessages;
import io.hotmoka.node.messages.GetIndexMessages;
import io.hotmoka.node.messages.GetIndexResultMessages;
import io.hotmoka.node.messages.GetInfoMessages;
import io.hotmoka.node.messages.GetInfoResultMessages;
import io.hotmoka.node.messages.GetManifestMessages;
import io.hotmoka.node.messages.GetManifestResultMessages;
import io.hotmoka.node.messages.GetPolledResponseMessages;
import io.hotmoka.node.messages.GetPolledResponseResultMessages;
import io.hotmoka.node.messages.GetRequestMessages;
import io.hotmoka.node.messages.GetRequestResultMessages;
import io.hotmoka.node.messages.GetResponseMessages;
import io.hotmoka.node.messages.GetResponseResultMessages;
import io.hotmoka.node.messages.GetStateMessages;
import io.hotmoka.node.messages.GetStateResultMessages;
import io.hotmoka.node.messages.GetTakamakaCodeMessages;
import io.hotmoka.node.messages.GetTakamakaCodeResultMessages;
import io.hotmoka.node.messages.PostConstructorCallTransactionMessages;
import io.hotmoka.node.messages.PostConstructorCallTransactionResultMessages;
import io.hotmoka.node.messages.PostInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.PostInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.PostJarStoreTransactionMessages;
import io.hotmoka.node.messages.PostJarStoreTransactionResultMessages;
import io.hotmoka.node.messages.PostStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.PostStaticMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.RunInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.RunInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.RunStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.RunStaticMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.api.AddConstructorCallTransactionMessage;
import io.hotmoka.node.messages.api.AddConstructorCallTransactionResultMessage;
import io.hotmoka.node.messages.api.AddGameteCreationTransactionMessage;
import io.hotmoka.node.messages.api.AddGameteCreationTransactionResultMessage;
import io.hotmoka.node.messages.api.AddInitializationTransactionMessage;
import io.hotmoka.node.messages.api.AddInitializationTransactionResultMessage;
import io.hotmoka.node.messages.api.AddInstanceMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.AddInstanceMethodCallTransactionResultMessage;
import io.hotmoka.node.messages.api.AddJarStoreInitialTransactionMessage;
import io.hotmoka.node.messages.api.AddJarStoreInitialTransactionResultMessage;
import io.hotmoka.node.messages.api.AddJarStoreTransactionMessage;
import io.hotmoka.node.messages.api.AddJarStoreTransactionResultMessage;
import io.hotmoka.node.messages.api.AddStaticMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.AddStaticMethodCallTransactionResultMessage;
import io.hotmoka.node.messages.api.EventMessage;
import io.hotmoka.node.messages.api.GetClassTagMessage;
import io.hotmoka.node.messages.api.GetClassTagResultMessage;
import io.hotmoka.node.messages.api.GetConfigMessage;
import io.hotmoka.node.messages.api.GetConfigResultMessage;
import io.hotmoka.node.messages.api.GetIndexMessage;
import io.hotmoka.node.messages.api.GetIndexResultMessage;
import io.hotmoka.node.messages.api.GetInfoMessage;
import io.hotmoka.node.messages.api.GetInfoResultMessage;
import io.hotmoka.node.messages.api.GetManifestMessage;
import io.hotmoka.node.messages.api.GetManifestResultMessage;
import io.hotmoka.node.messages.api.GetPolledResponseMessage;
import io.hotmoka.node.messages.api.GetPolledResponseResultMessage;
import io.hotmoka.node.messages.api.GetRequestMessage;
import io.hotmoka.node.messages.api.GetRequestResultMessage;
import io.hotmoka.node.messages.api.GetResponseMessage;
import io.hotmoka.node.messages.api.GetResponseResultMessage;
import io.hotmoka.node.messages.api.GetStateMessage;
import io.hotmoka.node.messages.api.GetStateResultMessage;
import io.hotmoka.node.messages.api.GetTakamakaCodeMessage;
import io.hotmoka.node.messages.api.GetTakamakaCodeResultMessage;
import io.hotmoka.node.messages.api.PostConstructorCallTransactionMessage;
import io.hotmoka.node.messages.api.PostConstructorCallTransactionResultMessage;
import io.hotmoka.node.messages.api.PostInstanceMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.PostInstanceMethodCallTransactionResultMessage;
import io.hotmoka.node.messages.api.PostJarStoreTransactionMessage;
import io.hotmoka.node.messages.api.PostJarStoreTransactionResultMessage;
import io.hotmoka.node.messages.api.PostStaticMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.PostStaticMethodCallTransactionResultMessage;
import io.hotmoka.node.messages.api.RunInstanceMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.RunInstanceMethodCallTransactionResultMessage;
import io.hotmoka.node.messages.api.RunStaticMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.RunStaticMethodCallTransactionResultMessage;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.api.FailedDeploymentException;
import io.hotmoka.websockets.beans.ExceptionMessages;
import io.hotmoka.websockets.beans.api.ExceptionMessage;
import io.hotmoka.websockets.beans.api.RpcMessage;
import io.hotmoka.websockets.client.AbstractRemote;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

/**
 * Shared implementation of a node that forwards all its calls to a remote service.
 */
@ThreadSafe
public class RemoteNodeImpl extends AbstractRemote implements RemoteNode {

	/**
	 * The manager of the subscriptions to the events occurring in this node.
	 */
	private final SubscriptionsManager subscriptions = SubscriptionsManagers.create();

	/**
	 * The prefix used in the log messages;
	 */
	private final String logPrefix;

	private final static Logger LOGGER = Logger.getLogger(RemoteNodeImpl.class.getName());

	/**
     * Builds the remote node.
     *
	 * @param uri the URI of the network service that gets bound to the remote node
	 * @param timeout the time (in milliseconds) allowed for the handshake to the network service;
	 *                beyond that threshold, a timeout exception is thrown
	 * @throws FailedDeploymentException if the remote node could not be deployed
     */
    public RemoteNodeImpl(URI uri, int timeout) throws FailedDeploymentException {
    	super(timeout);

    	this.logPrefix = "node remote(" + uri + "): ";

    	addSession(GET_INFO_ENDPOINT, uri, GetInfoEndpoint::new);
    	addSession(GET_CONFIG_ENDPOINT, uri, GetConfigEndpoint::new);
    	addSession(GET_TAKAMAKA_CODE_ENDPOINT, uri, GetTakamakaCodeEndpoint::new);
    	addSession(GET_MANIFEST_ENDPOINT, uri, GetManifestEndpoint::new);
    	addSession(GET_CLASS_TAG_ENDPOINT, uri, GetClassTagEndpoint::new);
    	addSession(GET_STATE_ENDPOINT, uri, GetStateEndpoint::new);
    	addSession(GET_INDEX_ENDPOINT, uri, GetIndexEndpoint::new);
    	addSession(GET_REQUEST_ENDPOINT, uri, GetRequestEndpoint::new);
    	addSession(GET_RESPONSE_ENDPOINT, uri, GetResponseEndpoint::new);
    	addSession(GET_POLLED_RESPONSE_ENDPOINT, uri, GetPolledResponseEndpoint::new);
    	addSession(ADD_GAMETE_CREATION_TRANSACTION_ENDPOINT, uri, AddGameteCreationTransactionEndpoint::new);
    	addSession(ADD_JAR_STORE_INITIAL_TRANSACTION_ENDPOINT, uri, AddJarStoreInitialTransactionEndpoint::new);
    	addSession(ADD_INITIALIZATION_TRANSACTION_ENDPOINT, uri, AddInitializationTransactionEndpoint::new);
    	addSession(ADD_JAR_STORE_TRANSACTION_ENDPOINT, uri, AddJarStoreTransactionEndpoint::new);
    	addSession(ADD_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT, uri, AddConstructorCallTransactionEndpoint::new);
    	addSession(ADD_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT, uri, AddInstanceMethodCallTransactionEndpoint::new);
    	addSession(ADD_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT, uri, AddStaticMethodCallTransactionEndpoint::new);
    	addSession(POST_JAR_STORE_TRANSACTION_ENDPOINT, uri, PostJarStoreTransactionEndpoint::new);
    	addSession(POST_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT, uri, PostConstructorCallTransactionEndpoint::new);
    	addSession(POST_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT, uri, PostInstanceMethodCallTransactionEndpoint::new);
    	addSession(POST_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT, uri, PostStaticMethodCallTransactionEndpoint::new);
    	addSession(RUN_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT, uri, RunInstanceMethodCallTransactionEndpoint::new);
    	addSession(RUN_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT, uri, RunStaticMethodCallTransactionEndpoint::new);
    	addSession(EVENTS_ENDPOINT, uri, EventsEndpoint::new);

    	LOGGER.info(logPrefix + "connected");
    }

	@Override
	protected void closeResources(CloseReason reason) {
		super.closeResources(reason);
		LOGGER.info(logPrefix + "closed with reason: " + reason);
	}

	@Override
	protected void notifyResult(RpcMessage message) {
		switch (message) {
		case GetInfoResultMessage gnirm -> onGetInfoResult(gnirm);
		case GetConfigResultMessage gccrm -> onGetConfigResult(gccrm);
		case GetTakamakaCodeResultMessage gtcrm -> onGetTakamakaCodeResult(gtcrm);
		case GetManifestResultMessage gmrm -> onGetManifestResult(gmrm);
		case GetClassTagResultMessage gctrm -> onGetClassTagResult(gctrm);
		case GetStateResultMessage gsrm -> onGetStateResult(gsrm);
		case GetIndexResultMessage girm -> onGetIndexResult(girm);
		case GetRequestResultMessage grrm -> onGetRequestResult(grrm);
		case GetResponseResultMessage grrm -> onGetResponseResult(grrm);
		case GetPolledResponseResultMessage gprrm -> onGetPolledResponseResult(gprrm);
		case AddJarStoreTransactionResultMessage ajstrm -> onAddJarStoreTransactionResult(ajstrm);
		case AddConstructorCallTransactionResultMessage acctrm -> onAddConstructorCallTransactionResult(acctrm);
		case AddInstanceMethodCallTransactionResultMessage aimctrm -> onAddInstanceMethodCallTransactionResult(aimctrm);
		case AddStaticMethodCallTransactionResultMessage asmctrm -> onAddStaticMethodCallTransactionResult(asmctrm);
		case AddGameteCreationTransactionResultMessage agctrm -> onAddGameteCreationTransactionResult(agctrm);
		case AddJarStoreInitialTransactionResultMessage ajsitrm -> onAddJarStoreInitialTransactionResult(ajsitrm);
		case AddInitializationTransactionResultMessage aitrm -> onAddInitializationTransactionResult(aitrm);
		case PostJarStoreTransactionResultMessage pjstrm -> onPostJarStoreTransactionResult(pjstrm);
		case PostConstructorCallTransactionResultMessage pcctrm -> onPostConstructorCallTransactionResult(pcctrm);
		case PostInstanceMethodCallTransactionResultMessage pimctrm -> onPostInstanceMethodCallTransactionResult(pimctrm);
		case PostStaticMethodCallTransactionResultMessage psmctrm -> onPostStaticMethodCallTransactionResult(psmctrm);
		case RunInstanceMethodCallTransactionResultMessage rimctrm -> onRunInstanceMethodCallTransactionResult(rimctrm);
		case RunStaticMethodCallTransactionResultMessage rsmctrm -> onRunStaticMethodCallTransactionResult(rsmctrm);
		default -> {
			if (message != null && !(message instanceof ExceptionMessage)) {
				LOGGER.warning("unexpected message of class " + message.getClass().getName());
				return;
			}
		}
		}

		super.notifyResult(message);
	}

	/**
	 * Sends the given message to the given endpoint. If it fails, it just logs
	 * the exception and continues.
	 * 
	 * @param endpoint the endpoint
	 * @param message the message
	 */
	private void sendObjectAsync(String endpoint, RpcMessage message) {
		try {
			sendObjectAsync(getSession(endpoint), message);
		}
		catch (IOException e) {
			LOGGER.warning("cannot send to " + endpoint + ": " + e.getMessage());
		}
	}

	@Override
	public NodeInfo getInfo() throws ClosedNodeException, TimeoutException, InterruptedException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendGetInfo(id);
		return waitForResult(id, GetInfoResultMessage.class);
	}

	/**
	 * Sends a {@link GetInfoMessage} to the node service.
	 * 
	 * @param id the identifier of the message
	 */
	protected void sendGetInfo(String id) {
		sendObjectAsync(GET_INFO_ENDPOINT, GetInfoMessages.of(id));
	}

	/**
	 * Hook called when a {@link GetInfoResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetInfoResult(GetInfoResultMessage message) {}

	private class GetInfoEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, GetInfoResultMessages.Decoder.class, GetInfoMessages.Encoder.class);		
		}
	}

	@Override
	public ConsensusConfig<?,?> getConfig() throws ClosedNodeException, TimeoutException, InterruptedException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendGetConfig(id);
		return waitForResult(id, GetConfigResultMessage.class);
	}

	/**
	 * Sends a {@link GetConfigMessage} to the node service.
	 * 
	 * @param id the identifier of the message
	 */
	protected void sendGetConfig(String id) {
		sendObjectAsync(GET_CONFIG_ENDPOINT, GetConfigMessages.of(id));
	}

	/**
	 * Hook called when a {@link GetConfigResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetConfigResult(GetConfigResultMessage message) {}

	private class GetConfigEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, GetConfigResultMessages.Decoder.class, GetConfigMessages.Encoder.class);		
		}
	}

	@Override
	public TransactionReference getTakamakaCode() throws UninitializedNodeException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendGetTakamakaCode(id);
		return waitForResult(id, GetTakamakaCodeResultMessage.class, UninitializedNodeException.class);
	}

	/**
	 * Sends a {@link GetTakamakaCodeMessage} to the node service.
	 * 
	 * @param id the identifier of the message
	 */
	protected void sendGetTakamakaCode(String id) {
		sendObjectAsync(GET_TAKAMAKA_CODE_ENDPOINT, GetTakamakaCodeMessages.of(id));
	}

	/**
	 * Hook called when a {@link GetTakamakaCodeResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetTakamakaCodeResult(GetTakamakaCodeResultMessage message) {}

	private class GetTakamakaCodeEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, GetTakamakaCodeResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetTakamakaCodeMessages.Encoder.class);		
		}
	}

	@Override
	public StorageReference getManifest() throws UninitializedNodeException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendGetManifest(id);
		return waitForResult(id, GetManifestResultMessage.class, UninitializedNodeException.class);
	}

	/**
	 * Sends a {@link GetManifestMessage} to the node service.
	 * 
	 * @param id the identifier of the message
	 */
	protected void sendGetManifest(String id) {
		sendObjectAsync(GET_MANIFEST_ENDPOINT, GetManifestMessages.of(id));
	}

	/**
	 * Hook called when a {@link GetManifestResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetManifestResult(GetManifestResultMessage message) {}

	private class GetManifestEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, GetManifestResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetManifestMessages.Encoder.class);		
		}
	}

	@Override
	public ClassTag getClassTag(StorageReference reference) throws UnknownReferenceException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendGetClassTag(reference, id);
		return waitForResult(id, GetClassTagResultMessage.class, UnknownReferenceException.class);
	}

	/**
	 * Sends a {@link GetClassTagMessage} to the node service.
	 * 
	 * @param reference the reference to the object whose class tag is required
	 * @param id the identifier of the message
	 */
	protected void sendGetClassTag(StorageReference reference, String id) {
		sendObjectAsync(GET_CLASS_TAG_ENDPOINT, GetClassTagMessages.of(reference, id));
	}

	/**
	 * Hook called when a {@link GetClassTagResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetClassTagResult(GetClassTagResultMessage message) {}

	private class GetClassTagEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, GetClassTagResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetClassTagMessages.Encoder.class);		
		}
	}

	@Override
	public Stream<Update> getState(StorageReference reference) throws UnknownReferenceException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendGetState(reference, id);
		return waitForResult(id, GetStateResultMessage.class, UnknownReferenceException.class);
	}

	/**
	 * Sends a {@link GetStateMessage} to the node service.
	 * 
	 * @param reference the reference to the object whose state is required
	 * @param id the identifier of the message
	 */
	protected void sendGetState(StorageReference reference, String id) {
		sendObjectAsync(GET_STATE_ENDPOINT, GetStateMessages.of(reference, id));
	}

	/**
	 * Hook called when a {@link GetStateResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetStateResult(GetStateResultMessage message) {}

	private class GetStateEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, GetStateResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetStateMessages.Encoder.class);		
		}
	}

	@Override
	public Stream<TransactionReference> getIndex(StorageReference object) throws UnknownReferenceException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendGetIndex(object, id);
		return waitForResult(id, GetIndexResultMessage.class, UnknownReferenceException.class);
	}

	/**
	 * Sends a {@link GetIndexMessage} to the node service.
	 * 
	 * @param reference the reference to the object whose state is required
	 * @param id the identifier of the message
	 */
	protected void sendGetIndex(StorageReference reference, String id) {
		sendObjectAsync(GET_INDEX_ENDPOINT, GetIndexMessages.of(reference, id));
	}

	/**
	 * Hook called when a {@link GetIndexResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetIndexResult(GetIndexResultMessage message) {}

	private class GetIndexEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, GetIndexResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetIndexMessages.Encoder.class);		
		}
	}

	@Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendGetRequest(reference, id);
		return waitForResult(id, GetRequestResultMessage.class, UnknownReferenceException.class);
	}

	/**
	 * Sends a {@link GetRequestMessage} to the node service.
	 * 
	 * @param reference the reference to the transaction whose request is required
	 * @param id the identifier of the message
	 */
	protected void sendGetRequest(TransactionReference reference, String id) {
		sendObjectAsync(GET_REQUEST_ENDPOINT, GetRequestMessages.of(reference, id));
	}

	/**
	 * Hook called when a {@link GetRequestResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetRequestResult(GetRequestResultMessage message) {}

	private class GetRequestEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, GetRequestResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetRequestMessages.Encoder.class);		
		}
	}

	@Override
	public TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendGetResponse(reference, id);
		return waitForResult(id, GetResponseResultMessage.class, UnknownReferenceException.class);
	}

	/**
	 * Sends a {@link GetResponseMessage} to the node service.
	 * 
	 * @param reference the reference to the transaction whose response is required
	 * @param id the identifier of the message
	 */
	protected void sendGetResponse(TransactionReference reference, String id) {
		sendObjectAsync(GET_RESPONSE_ENDPOINT, GetResponseMessages.of(reference, id));
	}

	/**
	 * Hook called when a {@link GetResponseResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetResponseResult(GetResponseResultMessage message) {}

	private class GetResponseEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, GetResponseResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetResponseMessages.Encoder.class);		
		}
	}

	@Override
	public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendGetPolledResponse(reference, id);
		return waitForResult(id, GetPolledResponseResultMessage.class, TransactionRejectedException.class);
	}

	/**
	 * Sends a {@link GetPolledResponseMessage} to the node service.
	 * 
	 * @param reference the reference to the transaction whose response is required
	 * @param id the identifier of the message
	 */
	protected void sendGetPolledResponse(TransactionReference reference, String id) {
		sendObjectAsync(GET_POLLED_RESPONSE_ENDPOINT, GetPolledResponseMessages.of(reference, id));
	}

	/**
	 * Hook called when a {@link GetPolledResponseResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetPolledResponseResult(GetPolledResponseResultMessage message) {}

	private class GetPolledResponseEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, GetPolledResponseResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetPolledResponseMessages.Encoder.class);		
		}
	}

	@Override
	public Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);		
		var id = nextId();
		sendRunInstanceMethodCallTransaction(request, id);
		return waitForResult(id, RunInstanceMethodCallTransactionResultMessage.class, TransactionRejectedException.class, TransactionException.class, CodeExecutionException.class);
	}

	/**
	 * Sends a {@link RunInstanceMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to run
	 * @param id the identifier of the message
	 */
	protected void sendRunInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, String id) {
		sendObjectAsync(RUN_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT, RunInstanceMethodCallTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when a {@link RunInstanceMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onRunInstanceMethodCallTransactionResult(RunInstanceMethodCallTransactionResultMessage message) {}

	private class RunInstanceMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, RunInstanceMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, RunInstanceMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendRunStaticMethodCallTransaction(request, id);
		return waitForResult(id, RunStaticMethodCallTransactionResultMessage.class, TransactionRejectedException.class, TransactionException.class, CodeExecutionException.class);
	}

	/**
	 * Sends a {@link RunStaticMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to run
	 * @param id the identifier of the message
	 */
	protected void sendRunStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, String id) {
		sendObjectAsync(RUN_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT, RunStaticMethodCallTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when a {@link RunStaticMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onRunStaticMethodCallTransactionResult(RunStaticMethodCallTransactionResultMessage message) {}

	private class RunStaticMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, RunStaticMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, RunStaticMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public Optional<StorageValue> addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendAddInstanceMethodCallTransaction(request, id);
		return waitForResult(id, AddInstanceMethodCallTransactionResultMessage.class, TransactionRejectedException.class, TransactionException.class, CodeExecutionException.class);
	}

	/**
	 * Sends an {@link AddInstanceMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 */
	protected void sendAddInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, String id) {
		sendObjectAsync(ADD_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT, AddInstanceMethodCallTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when an {@link AddInstanceMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddInstanceMethodCallTransactionResult(AddInstanceMethodCallTransactionResultMessage message) {}

	private class AddInstanceMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, AddInstanceMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddInstanceMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public Optional<StorageValue> addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendAddStaticMethodCallTransaction(request, id);
		return waitForResult(id, AddStaticMethodCallTransactionResultMessage.class, TransactionRejectedException.class, TransactionException.class, CodeExecutionException.class);
	}

	/**
	 * Sends an {@link AddStaticMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 */
	protected void sendAddStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, String id) {
		sendObjectAsync(ADD_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT, AddStaticMethodCallTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when an {@link AddStaticMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddStaticMethodCallTransactionResult(AddStaticMethodCallTransactionResultMessage message) {}

	private class AddStaticMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, AddStaticMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddStaticMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendAddConstructorCallTransaction(request, id);
		return waitForResult(id, AddConstructorCallTransactionResultMessage.class, TransactionRejectedException.class, TransactionException.class, CodeExecutionException.class);
	}

	/**
	 * Sends an {@link AddConstructorCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 */
	protected void sendAddConstructorCallTransaction(ConstructorCallTransactionRequest request, String id) {
		sendObjectAsync(ADD_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT, AddConstructorCallTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when an {@link AddConstructorCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddConstructorCallTransactionResult(AddConstructorCallTransactionResultMessage message) {}

	private class AddConstructorCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, AddConstructorCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddConstructorCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendAddJarStoreTransaction(request, id);
		return waitForResult(id, AddJarStoreTransactionResultMessage.class, TransactionRejectedException.class, TransactionException.class);
	}

	/**
	 * Sends an {@link AddJarStoreTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 */
	protected void sendAddJarStoreTransaction(JarStoreTransactionRequest request, String id) {
		sendObjectAsync(ADD_JAR_STORE_TRANSACTION_ENDPOINT, AddJarStoreTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when an {@link AddJarStoreTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddJarStoreTransactionResult(AddJarStoreTransactionResultMessage message) {}

	private class AddJarStoreTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, AddJarStoreTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddJarStoreTransactionMessages.Encoder.class);
		}
	}

	@Override
	public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendAddGameteCreationTransaction(request, id);
		return waitForResult(id, AddGameteCreationTransactionResultMessage.class, TransactionRejectedException.class);
	}

	/**
	 * Sends an {@link AddGameteCreationTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 */
	protected void sendAddGameteCreationTransaction(GameteCreationTransactionRequest request, String id) {
		sendObjectAsync(ADD_GAMETE_CREATION_TRANSACTION_ENDPOINT, AddGameteCreationTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when an {@link AddGameteCreationTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddGameteCreationTransactionResult(AddGameteCreationTransactionResultMessage message) {}

	private class AddGameteCreationTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, AddGameteCreationTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddGameteCreationTransactionMessages.Encoder.class);
		}
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendAddJarStoreInitialTransaction(request, id);
		return waitForResult(id, AddJarStoreInitialTransactionResultMessage.class, TransactionRejectedException.class);
	}

	/**
	 * Sends an {@link AddJarStoreInitialTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 */
	protected void sendAddJarStoreInitialTransaction(JarStoreInitialTransactionRequest request, String id) {
		sendObjectAsync(ADD_JAR_STORE_INITIAL_TRANSACTION_ENDPOINT, AddJarStoreInitialTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when an {@link AddJarStoreInitialTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddJarStoreInitialTransactionResult(AddJarStoreInitialTransactionResultMessage message) {}

	private class AddJarStoreInitialTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, AddJarStoreInitialTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddJarStoreInitialTransactionMessages.Encoder.class);
		}
	}

	@Override
	public final void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendAddInitializationTransaction(request, id);
		waitForResult(id, AddInitializationTransactionResultMessage.class, TransactionRejectedException.class);
	}

	/**
	 * Sends an {@link AddInitializationTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 */
	protected void sendAddInitializationTransaction(InitializationTransactionRequest request, String id) {
		sendObjectAsync(ADD_INITIALIZATION_TRANSACTION_ENDPOINT, AddInitializationTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when an {@link AddInitializationTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddInitializationTransactionResult(AddInitializationTransactionResultMessage message) {}

	private class AddInitializationTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, AddInitializationTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddInitializationTransactionMessages.Encoder.class);
		}
	}

	@Override
	public ConstructorFuture postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendPostConstructorCallTransaction(request, id);
		return CodeFutures.ofConstructor(waitForResult(id, PostConstructorCallTransactionResultMessage.class, TransactionRejectedException.class), this);
	}

	/**
	 * Sends a {@link PostConstructorCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to post
	 * @param id the identifier of the message
	 */
	protected void sendPostConstructorCallTransaction(ConstructorCallTransactionRequest request, String id) {
		sendObjectAsync(POST_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT, PostConstructorCallTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when a {@link PostConstructorCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onPostConstructorCallTransactionResult(PostConstructorCallTransactionResultMessage message) {}

	private class PostConstructorCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, PostConstructorCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, PostConstructorCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public MethodFuture postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendPostInstanceMethodCallTransaction(request, id);
		return CodeFutures.ofMethod(waitForResult(id, PostInstanceMethodCallTransactionResultMessage.class, TransactionRejectedException.class), this);
	}

	/**
	 * Sends a {@link PostInstanceMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to post
	 * @param id the identifier of the message
	 */
	protected void sendPostInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, String id) {
		sendObjectAsync(POST_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT, PostInstanceMethodCallTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when a {@link PostInstanceMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onPostInstanceMethodCallTransactionResult(PostInstanceMethodCallTransactionResultMessage message) {}

	private class PostInstanceMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, PostInstanceMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, PostInstanceMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public MethodFuture postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendPostStaticMethodCallTransaction(request, id);
		return CodeFutures.ofMethod(waitForResult(id, PostStaticMethodCallTransactionResultMessage.class, TransactionRejectedException.class), this);
	}

	/**
	 * Sends a {@link PostStaticMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to post
	 * @param id the identifier of the message
	 */
	protected void sendPostStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, String id) {
		sendObjectAsync(POST_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT, PostStaticMethodCallTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when a {@link PostStaticMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onPostStaticMethodCallTransactionResult(PostStaticMethodCallTransactionResultMessage message) {}

	private class PostStaticMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, PostStaticMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, PostStaticMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public JarFuture postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, ClosedNodeException, InterruptedException, TimeoutException {
		ensureIsOpen(ClosedNodeException::new);
		var id = nextId();
		sendPostJarStoreTransaction(request, id);
		return JarFutures.of(waitForResult(id, PostJarStoreTransactionResultMessage.class, TransactionRejectedException.class), this);
	}

	/**
	 * Sends a {@link PostJarStoreTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to post
	 * @param id the identifier of the message
	 */
	protected void sendPostJarStoreTransaction(JarStoreTransactionRequest request, String id) {
		sendObjectAsync(POST_JAR_STORE_TRANSACTION_ENDPOINT, PostJarStoreTransactionMessages.of(request, id));
	}

	/**
	 * Hook called when a {@link PostJarStoreTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onPostJarStoreTransactionResult(PostJarStoreTransactionResultMessage message) {}

	private class PostJarStoreTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, PostJarStoreTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, PostJarStoreTransactionMessages.Encoder.class);
		}
	}

	private class EventsEndpoint extends Endpoint {

		@Override
		public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, RemoteNodeImpl.this::notifyEvent);
		}

		@Override
		protected Session deployAt(URI uri) throws FailedDeploymentException {
			return deployAt(uri, EventMessages.Decoder.class);
		}
	}

	@Override
	public final Subscription subscribeToEvents(StorageReference creator, BiConsumer<StorageReference, StorageReference> handler) throws ClosedNodeException {
		ensureIsOpen(ClosedNodeException::new);
		return subscriptions.subscribeToEvents(creator, handler);
	}

	/**
	 * Notifies the given event message to all event handlers for the creator of the event.
	 * 
	 * @param message the event message
	 */
	private void notifyEvent(EventMessage message) {
		StorageReference event = message.getEvent();
		StorageReference creator = message.getCreator();
		LOGGER.info(logPrefix + "received event " + event + " with creator " + creator);
		subscriptions.notifyEvent(creator, event);
	}
}