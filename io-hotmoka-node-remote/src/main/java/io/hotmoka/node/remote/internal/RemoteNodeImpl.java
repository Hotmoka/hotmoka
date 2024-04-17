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
import static io.hotmoka.node.service.api.NodeService.GET_CONSENSUS_CONFIG_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_MANIFEST_ENDPOINT;
import static io.hotmoka.node.service.api.NodeService.GET_NODE_INFO_ENDPOINT;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.nodes.NodeInfo;
import io.hotmoka.beans.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.api.requests.InitializationTransactionRequest;
import io.hotmoka.beans.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.api.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.updates.ClassTag;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.CodeSuppliers;
import io.hotmoka.node.JarSuppliers;
import io.hotmoka.node.SubscriptionsManagers;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.CodeSupplier;
import io.hotmoka.node.api.JarSupplier;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.SubscriptionsManager;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
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
import io.hotmoka.node.messages.GetConsensusConfigMessages;
import io.hotmoka.node.messages.GetConsensusConfigResultMessages;
import io.hotmoka.node.messages.GetManifestMessages;
import io.hotmoka.node.messages.GetManifestResultMessages;
import io.hotmoka.node.messages.GetNodeInfoMessages;
import io.hotmoka.node.messages.GetNodeInfoResultMessages;
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
import io.hotmoka.node.messages.api.GetConsensusConfigMessage;
import io.hotmoka.node.messages.api.GetConsensusConfigResultMessage;
import io.hotmoka.node.messages.api.GetManifestMessage;
import io.hotmoka.node.messages.api.GetManifestResultMessage;
import io.hotmoka.node.messages.api.GetNodeInfoMessage;
import io.hotmoka.node.messages.api.GetNodeInfoResultMessage;
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
import io.hotmoka.websockets.beans.ExceptionMessages;
import io.hotmoka.websockets.beans.api.ExceptionMessage;
import io.hotmoka.websockets.beans.api.RpcMessage;
import io.hotmoka.websockets.client.AbstractRemote;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

/**
 * Shared implementation of a node that forwards all its calls to a remote service.
 */
@ThreadSafe
public class RemoteNodeImpl extends AbstractRemote<NodeException> implements RemoteNode {

	/**
	 * The manager of the subscriptions to the events occurring in this node.
	 */
	private final SubscriptionsManager subscriptions = SubscriptionsManagers.mk();

	/**
	 * The prefix used in the log messages;
	 */
	private final String logPrefix;

	private final static Logger LOGGER = Logger.getLogger(RemoteNodeImpl.class.getName());

	/**
     * Builds the remote node.
     *
	 * @param uri the URI of the network service that gets bound to the remote node
	 * @param timeout the time (in milliseconds) allowed for a call to the network service;
	 *                beyond that threshold, a timeout exception is thrown
	 * @throws DeploymentException if the remote node could not be deployed
	 * @throws IOException if the remote node could not be created
     */
    public RemoteNodeImpl(URI uri, long timeout) throws IOException, DeploymentException {
    	super(timeout);

    	this.logPrefix = "node remote(" + uri + "): ";

    	addSession(GET_NODE_INFO_ENDPOINT, uri, GetNodeInfoEndpoint::new);
    	addSession(GET_CONSENSUS_CONFIG_ENDPOINT, uri, GetConsensusConfigEndpoint::new);
    	addSession(GET_TAKAMAKA_CODE_ENDPOINT, uri, GetTakamakaCodeEndpoint::new);
    	addSession(GET_MANIFEST_ENDPOINT, uri, GetManifestEndpoint::new);
    	addSession(GET_CLASS_TAG_ENDPOINT, uri, GetClassTagEndpoint::new);
    	addSession(GET_STATE_ENDPOINT, uri, GetStateEndpoint::new);
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
	protected ClosedNodeException mkExceptionIfClosed() {
		return new ClosedNodeException();
	}

	@Override
	protected NodeException mkException(Exception cause) {
		return cause instanceof NodeException ne ? ne : new NodeException(cause);
	}

	@Override
	protected void closeResources(CloseReason reason) throws NodeException, InterruptedException {
		super.closeResources(reason);
		LOGGER.info(logPrefix + "closed with reason: " + reason);
	}

	@Override
	protected void notifyResult(RpcMessage message) {
		if (message instanceof GetNodeInfoResultMessage gnirm)
			onGetNodeInfoResult(gnirm);
		else if (message instanceof GetConsensusConfigResultMessage gccrm)
			onGetConsensusConfigResult(gccrm);
		else if (message instanceof GetTakamakaCodeResultMessage gtcrm)
			onGetTakamakaCodeResult(gtcrm);
		else if (message instanceof GetManifestResultMessage gmrm)
			onGetManifestResult(gmrm);
		else if (message instanceof GetClassTagResultMessage gctrm)
			onGetClassTagResult(gctrm);
		else if (message instanceof GetStateResultMessage gsrm)
			onGetStateResult(gsrm);
		else if (message instanceof GetRequestResultMessage grrm)
			onGetRequestResult(grrm);
		else if (message instanceof GetResponseResultMessage grrm)
			onGetResponseResult(grrm);
		else if (message instanceof GetPolledResponseResultMessage gprrm)
			onGetPolledResponseResult(gprrm);
		else if (message instanceof AddJarStoreTransactionResultMessage ajstrm)
			onAddJarStoreTransactionResult(ajstrm);
		else if (message instanceof AddConstructorCallTransactionResultMessage acctrm)
			onAddConstructorCallTransactionResult(acctrm);
		else if (message instanceof AddInstanceMethodCallTransactionResultMessage aimctrm)
			onAddInstanceMethodCallTransactionResult(aimctrm);
		else if (message instanceof AddStaticMethodCallTransactionResultMessage asmctrm)
			onAddStaticMethodCallTransactionResult(asmctrm);
		else if (message instanceof AddGameteCreationTransactionResultMessage agctrm)
			onAddGameteCreationTransactionResult(agctrm);
		else if (message instanceof AddJarStoreInitialTransactionResultMessage ajsitrm)
			onAddJarStoreInitialTransactionResult(ajsitrm);
		else if (message instanceof AddInitializationTransactionResultMessage aitrm)
			onAddInitializationTransactionResult(aitrm);
		else if (message instanceof PostJarStoreTransactionResultMessage pjstrm)
			onPostJarStoreTransactionResult(pjstrm);
		else if (message instanceof PostConstructorCallTransactionResultMessage pcctrm)
			onPostConstructorCallTransactionResult(pcctrm);
		else if (message instanceof PostInstanceMethodCallTransactionResultMessage pimctrm)
			onPostInstanceMethodCallTransactionResult(pimctrm);
		else if (message instanceof PostStaticMethodCallTransactionResultMessage psmctrm)
			onPostStaticMethodCallTransactionResult(psmctrm);
		else if (message instanceof RunInstanceMethodCallTransactionResultMessage rimctrm)
			onRunInstanceMethodCallTransactionResult(rimctrm);
		else if (message instanceof RunStaticMethodCallTransactionResultMessage rsmctrm)
			onRunStaticMethodCallTransactionResult(rsmctrm);
		else if (message != null && !(message instanceof ExceptionMessage)) {
			LOGGER.warning("unexpected message of class " + message.getClass().getName());
			return;
		}

		super.notifyResult(message);
	}

	/**
	 * Determines if the given exception message deals with an exception that all
	 * methods of a node are expected to throw. These are
	 * {@code java.lang.TimeoutException}, {@code java.lang.InterruptedException}
	 * and {@link NodeException}.
	 * 
	 * @param message the message
	 * @return true if and only if that condition holds
	 */
	private boolean processStandardExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TimeoutException.class.isAssignableFrom(clazz) ||
			InterruptedException.class.isAssignableFrom(clazz) ||
			NodeException.class.isAssignableFrom(clazz);
	}

	private RuntimeException unexpectedException(Exception e) {
		LOGGER.log(Level.SEVERE, logPrefix + "unexpected exception", e);
		return new RuntimeException("Unexpected exception", e);
	}

	@Override
	public NodeInfo getNodeInfo() throws NodeException, TimeoutException, InterruptedException {
		ensureIsOpen();
		var id = nextId();
		sendGetNodeInfo(id);
		try {
			return waitForResult(id, this::processGetNodeInfoSuccess, this::processStandardExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link GetNodeInfoMessage} to the node service.
	 * 
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendGetNodeInfo(String id) throws NodeException {
		try {
			sendObjectAsync(getSession(GET_NODE_INFO_ENDPOINT), GetNodeInfoMessages.of(id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private NodeInfo processGetNodeInfoSuccess(RpcMessage message) {
		return message instanceof GetNodeInfoResultMessage gnirm ? gnirm.get() : null;
	}

	/**
	 * Hook called when a {@link GetNodeInfoResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetNodeInfoResult(GetNodeInfoResultMessage message) {}

	private class GetNodeInfoEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, GetNodeInfoResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetNodeInfoMessages.Encoder.class);		
		}
	}

	@Override
	public String getConsensusConfig() throws NodeException, TimeoutException, InterruptedException {
		ensureIsOpen();
		var id = nextId();
		sendGetConsensusConfig(id);
		try {
			return waitForResult(id, this::processGetConsensusConfigSuccess, this::processStandardExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link GetConsensusConfigMessage} to the node service.
	 * 
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendGetConsensusConfig(String id) throws NodeException {
		try {
			sendObjectAsync(getSession(GET_CONSENSUS_CONFIG_ENDPOINT), GetConsensusConfigMessages.of(id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private String processGetConsensusConfigSuccess(RpcMessage message) {
		return message instanceof GetConsensusConfigResultMessage gccrm ? gccrm.get() : null;
	}

	/**
	 * Hook called when a {@link GetConsensusConfigResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetConsensusConfigResult(GetConsensusConfigResultMessage message) {}

	private class GetConsensusConfigEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, GetConsensusConfigResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetConsensusConfigMessages.Encoder.class);		
		}
	}

	@Override
	public TransactionReference getTakamakaCode() throws NoSuchElementException, NodeException, InterruptedException, TimeoutException { // TODO: remove NoSuchElement at the end
		ensureIsOpen();
		var id = nextId();
		sendGetTakamakaCode(id);
		try {
			return waitForResult(id, this::processGetTakamakaCodeSuccess, this::processGetTakamakaCodeExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link GetTakamakaCodeMessage} to the node service.
	 * 
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendGetTakamakaCode(String id) throws NodeException {
		try {
			sendObjectAsync(getSession(GET_TAKAMAKA_CODE_ENDPOINT), GetTakamakaCodeMessages.of(id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private TransactionReference processGetTakamakaCodeSuccess(RpcMessage message) {
		return message instanceof GetTakamakaCodeResultMessage gtcrm ? gtcrm.get() : null;
	}

	private boolean processGetTakamakaCodeExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return NoSuchElementException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link GetTakamakaCodeResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetTakamakaCodeResult(GetTakamakaCodeResultMessage message) {}

	private class GetTakamakaCodeEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, GetTakamakaCodeResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetTakamakaCodeMessages.Encoder.class);		
		}
	}

	@Override
	public Optional<StorageReference> getManifest() throws NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendGetManifest(id);
		try {
			return waitForResult(id, this::processGetManifestSuccess, this::processGetManifestExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link GetManifestMessage} to the node service.
	 * 
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendGetManifest(String id) throws NodeException {
		try {
			sendObjectAsync(getSession(GET_MANIFEST_ENDPOINT), GetManifestMessages.of(id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private Optional<StorageReference> processGetManifestSuccess(RpcMessage message) {
		return message instanceof GetManifestResultMessage gmrm ? gmrm.get() : null;
	}

	private boolean processGetManifestExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return NoSuchElementException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link GetManifestResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetManifestResult(GetManifestResultMessage message) {}

	private class GetManifestEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, GetManifestResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetManifestMessages.Encoder.class);		
		}
	}

	@Override
	public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendGetClassTag(reference, id);
		try {
			return waitForResult(id, this::processGetClassTagSuccess, this::processGetClassTagExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link GetClassTagMessage} to the node service.
	 * 
	 * @param reference the reference to the object whose class tag is required
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendGetClassTag(StorageReference reference, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(GET_CLASS_TAG_ENDPOINT), GetClassTagMessages.of(reference, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private ClassTag processGetClassTagSuccess(RpcMessage message) {
		return message instanceof GetClassTagResultMessage gctrm ? gctrm.get() : null;
	}

	private boolean processGetClassTagExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return NoSuchElementException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link GetClassTagResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetClassTagResult(GetClassTagResultMessage message) {}

	private class GetClassTagEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, GetClassTagResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetClassTagMessages.Encoder.class);		
		}
	}

	@Override
	public Stream<Update> getState(StorageReference reference) throws NoSuchElementException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendGetState(reference, id);
		try {
			return waitForResult(id, this::processGetStateSuccess, this::processGetStateExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link GetStateMessage} to the node service.
	 * 
	 * @param reference the reference to the object whose state is required
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendGetState(StorageReference reference, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(GET_STATE_ENDPOINT), GetStateMessages.of(reference, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private Stream<Update> processGetStateSuccess(RpcMessage message) {
		return message instanceof GetStateResultMessage gsrm ? gsrm.get() : null;
	}

	private boolean processGetStateExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return NoSuchElementException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link GetStateResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetStateResult(GetStateResultMessage message) {}

	private class GetStateEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, GetStateResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetStateMessages.Encoder.class);		
		}
	}

	@Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendGetRequest(reference, id);
		try {
			return waitForResult(id, this::processGetRequestSuccess, this::processGetRequestExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link GetRequestMessage} to the node service.
	 * 
	 * @param reference the reference to the transaction whose request is required
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendGetRequest(TransactionReference reference, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(GET_REQUEST_ENDPOINT), GetRequestMessages.of(reference, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private TransactionRequest<?> processGetRequestSuccess(RpcMessage message) {
		return message instanceof GetRequestResultMessage grrm ? grrm.get() : null;
	}

	private boolean processGetRequestExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return NoSuchElementException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link GetRequestResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetRequestResult(GetRequestResultMessage message) {}

	private class GetRequestEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, GetRequestResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetRequestMessages.Encoder.class);		
		}
	}

	@Override
	public TransactionResponse getResponse(TransactionReference reference) throws NoSuchElementException, TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendGetResponse(reference, id);
		try {
			return waitForResult(id, this::processGetResponseSuccess, this::processGetResponseExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link GetResponseMessage} to the node service.
	 * 
	 * @param reference the reference to the transaction whose response is required
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendGetResponse(TransactionReference reference, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(GET_RESPONSE_ENDPOINT), GetResponseMessages.of(reference, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private TransactionResponse processGetResponseSuccess(RpcMessage message) {
		return message instanceof GetResponseResultMessage grrm ? grrm.get() : null;
	}

	private boolean processGetResponseExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return NoSuchElementException.class.isAssignableFrom(clazz) ||
			TransactionRejectedException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link GetResponseResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetResponseResult(GetResponseResultMessage message) {}

	private class GetResponseEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, GetResponseResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetResponseMessages.Encoder.class);		
		}
	}

	@Override
	public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendGetPolledResponse(reference, id);
		try {
			return waitForResult(id, this::processGetPolledResponseSuccess, this::processGetPolledResponseExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link GetPolledResponseMessage} to the node service.
	 * 
	 * @param reference the reference to the transaction whose response is required
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendGetPolledResponse(TransactionReference reference, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(GET_POLLED_RESPONSE_ENDPOINT), GetPolledResponseMessages.of(reference, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private TransactionResponse processGetPolledResponseSuccess(RpcMessage message) {
		return message instanceof GetPolledResponseResultMessage gprrm ? gprrm.get() : null;
	}

	private boolean processGetPolledResponseExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link GetPolledResponseResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onGetPolledResponseResult(GetPolledResponseResultMessage message) {}

	private class GetPolledResponseEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, GetPolledResponseResultMessages.Decoder.class, ExceptionMessages.Decoder.class, GetPolledResponseMessages.Encoder.class);		
		}
	}

	@Override
	public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendRunInstanceMethodCallTransaction(request, id);
		try {
			return waitForResult(id, this::processRunInstanceMethodCallTransactionSuccess, this::processRunInstanceMethodCallTransactionExceptions).orElse(null);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link RunInstanceMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to run
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendRunInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(RUN_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT), RunInstanceMethodCallTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private Optional<StorageValue> processRunInstanceMethodCallTransactionSuccess(RpcMessage message) {
		return message instanceof RunInstanceMethodCallTransactionResultMessage rimctrm ? rimctrm.get() : null;
	}

	private boolean processRunInstanceMethodCallTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			TransactionException.class.isAssignableFrom(clazz) ||
			CodeExecutionException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link RunInstanceMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onRunInstanceMethodCallTransactionResult(RunInstanceMethodCallTransactionResultMessage message) {}

	private class RunInstanceMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, RunInstanceMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, RunInstanceMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendRunStaticMethodCallTransaction(request, id);
		try {
			return waitForResult(id, this::processRunStaticMethodCallTransactionSuccess, this::processRunStaticMethodCallTransactionExceptions).orElse(null);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link RunStaticMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to run
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendRunStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(RUN_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT), RunStaticMethodCallTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private Optional<StorageValue> processRunStaticMethodCallTransactionSuccess(RpcMessage message) {
		return message instanceof RunStaticMethodCallTransactionResultMessage rimctrm ? rimctrm.get() : null;
	}

	private boolean processRunStaticMethodCallTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			TransactionException.class.isAssignableFrom(clazz) ||
			CodeExecutionException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link RunStaticMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onRunStaticMethodCallTransactionResult(RunStaticMethodCallTransactionResultMessage message) {}

	private class RunStaticMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, RunStaticMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, RunStaticMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendAddInstanceMethodCallTransaction(request, id);
		try {
			return waitForResult(id, this::processAddInstanceMethodCallTransactionSuccess, this::processAddInstanceMethodCallTransactionExceptions).orElse(null);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends an {@link AddInstanceMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendAddInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(ADD_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT), AddInstanceMethodCallTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private Optional<StorageValue> processAddInstanceMethodCallTransactionSuccess(RpcMessage message) {
		return message instanceof AddInstanceMethodCallTransactionResultMessage aimctrm ? aimctrm.get() : null;
	}

	private boolean processAddInstanceMethodCallTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			TransactionException.class.isAssignableFrom(clazz) ||
			CodeExecutionException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when an {@link AddInstanceMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddInstanceMethodCallTransactionResult(AddInstanceMethodCallTransactionResultMessage message) {}

	private class AddInstanceMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, AddInstanceMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddInstanceMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendAddStaticMethodCallTransaction(request, id);
		try {
			return waitForResult(id, this::processAddStaticMethodCallTransactionSuccess, this::processAddStaticMethodCallTransactionExceptions).orElse(null);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends an {@link AddStaticMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendAddStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(ADD_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT), AddStaticMethodCallTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private Optional<StorageValue> processAddStaticMethodCallTransactionSuccess(RpcMessage message) {
		return message instanceof AddStaticMethodCallTransactionResultMessage aimctrm ? aimctrm.get() : null;
	}

	private boolean processAddStaticMethodCallTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			TransactionException.class.isAssignableFrom(clazz) ||
			CodeExecutionException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when an {@link AddStaticMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddStaticMethodCallTransactionResult(AddStaticMethodCallTransactionResultMessage message) {}

	private class AddStaticMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, AddStaticMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddStaticMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendAddConstructorCallTransaction(request, id);
		try {
			return waitForResult(id, this::processAddConstructorCallTransactionSuccess, this::processAddConstructorCallTransactionExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends an {@link AddConstructorCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendAddConstructorCallTransaction(ConstructorCallTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(ADD_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT), AddConstructorCallTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private StorageReference processAddConstructorCallTransactionSuccess(RpcMessage message) {
		return message instanceof AddConstructorCallTransactionResultMessage acctrm ? acctrm.get() : null;
	}

	private boolean processAddConstructorCallTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			TransactionException.class.isAssignableFrom(clazz) ||
			CodeExecutionException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when an {@link AddConstructorCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddConstructorCallTransactionResult(AddConstructorCallTransactionResultMessage message) {}

	private class AddConstructorCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, AddConstructorCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddConstructorCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendAddJarStoreTransaction(request, id);
		try {
			return waitForResult(id, this::processAddJarStoreTransactionSuccess, this::processAddJarStoreTransactionExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends an {@link AddJarStoreTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendAddJarStoreTransaction(JarStoreTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(ADD_JAR_STORE_TRANSACTION_ENDPOINT), AddJarStoreTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private TransactionReference processAddJarStoreTransactionSuccess(RpcMessage message) {
		return message instanceof AddJarStoreTransactionResultMessage ajstrm ? ajstrm.get() : null;
	}

	private boolean processAddJarStoreTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			TransactionException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when an {@link AddJarStoreTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddJarStoreTransactionResult(AddJarStoreTransactionResultMessage message) {}

	private class AddJarStoreTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, AddJarStoreTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddJarStoreTransactionMessages.Encoder.class);
		}
	}

	@Override
	public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendAddGameteCreationTransaction(request, id);
		try {
			return waitForResult(id, this::processAddGameteCreationTransactionSuccess, this::processAddGameteCreationTransactionExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends an {@link AddGameteCreationTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendAddGameteCreationTransaction(GameteCreationTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(ADD_GAMETE_CREATION_TRANSACTION_ENDPOINT), AddGameteCreationTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private StorageReference processAddGameteCreationTransactionSuccess(RpcMessage message) {
		return message instanceof AddGameteCreationTransactionResultMessage gctrm ? gctrm.get() : null;
	}

	private boolean processAddGameteCreationTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when an {@link AddGameteCreationTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddGameteCreationTransactionResult(AddGameteCreationTransactionResultMessage message) {}

	private class AddGameteCreationTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, AddGameteCreationTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddGameteCreationTransactionMessages.Encoder.class);
		}
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendAddJarStoreInitialTransaction(request, id);
		try {
			return waitForResult(id, this::processAddJarStoreInitialTransactionSuccess, this::processAddJarStoreInitialTransactionExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends an {@link AddJarStoreInitialTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendAddJarStoreInitialTransaction(JarStoreInitialTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(ADD_JAR_STORE_INITIAL_TRANSACTION_ENDPOINT), AddJarStoreInitialTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private TransactionReference processAddJarStoreInitialTransactionSuccess(RpcMessage message) {
		return message instanceof AddJarStoreInitialTransactionResultMessage ajstrm ? ajstrm.get() : null;
	}

	private boolean processAddJarStoreInitialTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when an {@link AddJarStoreInitialTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddJarStoreInitialTransactionResult(AddJarStoreInitialTransactionResultMessage message) {}

	private class AddJarStoreInitialTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, AddJarStoreInitialTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddJarStoreInitialTransactionMessages.Encoder.class);
		}
	}

	@Override
	public final void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendAddInitializationTransaction(request, id);
		try {
			waitForResult(id, this::processAddInitializationTransactionSuccess, this::processAddInitializationTransactionExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends an {@link AddInitializationTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendAddInitializationTransaction(InitializationTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(ADD_INITIALIZATION_TRANSACTION_ENDPOINT), AddInitializationTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private Boolean processAddInitializationTransactionSuccess(RpcMessage message) {
		return message instanceof AddInitializationTransactionResultMessage aitrm ? Boolean.TRUE : null;
	}

	private boolean processAddInitializationTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when an {@link AddInitializationTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onAddInitializationTransactionResult(AddInitializationTransactionResultMessage message) {}

	private class AddInitializationTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, AddInitializationTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, AddInitializationTransactionMessages.Encoder.class);
		}
	}

	@Override
	public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendPostConstructorCallTransaction(request, id);
		try {
			return waitForResult(id, this::processPostConstructorCallTransactionSuccess, this::processPostConstructorCallTransactionExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link PostConstructorCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to post
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendPostConstructorCallTransaction(ConstructorCallTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(POST_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT), PostConstructorCallTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private CodeSupplier<StorageReference> processPostConstructorCallTransactionSuccess(RpcMessage message) {
		return message instanceof PostConstructorCallTransactionResultMessage pcctrm ? CodeSuppliers.ofConstructor(pcctrm.get(), this) : null;
	}

	private boolean processPostConstructorCallTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link PostConstructorCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onPostConstructorCallTransactionResult(PostConstructorCallTransactionResultMessage message) {}

	private class PostConstructorCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, PostConstructorCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, PostConstructorCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendPostInstanceMethodCallTransaction(request, id);
		try {
			return waitForResult(id, this::processPostInstanceMethodCallTransactionSuccess, this::processPostInstanceMethodCallTransactionExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link PostInstanceMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to post
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendPostInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(POST_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT), PostInstanceMethodCallTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private CodeSupplier<StorageValue> processPostInstanceMethodCallTransactionSuccess(RpcMessage message) {
		return message instanceof PostInstanceMethodCallTransactionResultMessage pimctrm ? CodeSuppliers.ofMethod(pimctrm.get(), this) : null;
	}

	private boolean processPostInstanceMethodCallTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link PostInstanceMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onPostInstanceMethodCallTransactionResult(PostInstanceMethodCallTransactionResultMessage message) {}

	private class PostInstanceMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, PostInstanceMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, PostInstanceMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendPostStaticMethodCallTransaction(request, id);
		try {
			return waitForResult(id, this::processPostStaticMethodCallTransactionSuccess, this::processPostStaticMethodCallTransactionExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link PostStaticMethodCallTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to post
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendPostStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(POST_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT), PostStaticMethodCallTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private CodeSupplier<StorageValue> processPostStaticMethodCallTransactionSuccess(RpcMessage message) {
		return message instanceof PostStaticMethodCallTransactionResultMessage psmctrm ? CodeSuppliers.ofMethod(psmctrm.get(), this) : null;
	}

	private boolean processPostStaticMethodCallTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link PostStaticMethodCallTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onPostStaticMethodCallTransactionResult(PostStaticMethodCallTransactionResultMessage message) {}

	private class PostStaticMethodCallTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, PostStaticMethodCallTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, PostStaticMethodCallTransactionMessages.Encoder.class);
		}
	}

	@Override
	public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		ensureIsOpen();
		var id = nextId();
		sendPostJarStoreTransaction(request, id);
		try {
			return waitForResult(id, this::processPostJarStoreTransactionSuccess, this::processPostJarStoreTransactionExceptions);
		}
		catch (RuntimeException | TimeoutException | InterruptedException | NodeException | TransactionRejectedException e) {
			throw e;
		}
		catch (Exception e) {
			throw unexpectedException(e);
		}
	}

	/**
	 * Sends a {@link PostJarStoreTransactionMessage} to the node service.
	 * 
	 * @param request the request of the transaction required to post
	 * @param id the identifier of the message
	 * @throws NodeException if the message could not be sent
	 */
	protected void sendPostJarStoreTransaction(JarStoreTransactionRequest request, String id) throws NodeException {
		try {
			sendObjectAsync(getSession(POST_JAR_STORE_TRANSACTION_ENDPOINT), PostJarStoreTransactionMessages.of(request, id));
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	private JarSupplier processPostJarStoreTransactionSuccess(RpcMessage message) {
		return message instanceof PostJarStoreTransactionResultMessage pjstrm ? JarSuppliers.of(pjstrm.get(), this) : null;
	}

	private boolean processPostJarStoreTransactionExceptions(ExceptionMessage message) {
		var clazz = message.getExceptionClass();
		return TransactionRejectedException.class.isAssignableFrom(clazz) ||
			processStandardExceptions(message);
	}

	/**
	 * Hook called when a {@link PostJarStoreTransactionResultMessage} has been received.
	 * 
	 * @param message the message
	 */
	protected void onPostJarStoreTransactionResult(PostJarStoreTransactionResultMessage message) {}

	private class PostJarStoreTransactionEndpoint extends Endpoint {

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, PostJarStoreTransactionResultMessages.Decoder.class, ExceptionMessages.Decoder.class, PostJarStoreTransactionMessages.Encoder.class);
		}
	}

	private class EventsEndpoint extends Endpoint {

		@Override
		public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (Consumer<EventMessage>) RemoteNodeImpl.this::notifyEvent);
		}

		@Override
		protected Session deployAt(URI uri) throws DeploymentException, IOException {
			return deployAt(uri, EventMessages.Decoder.class);
		}
	}

	@Override
	public final Subscription subscribeToEvents(StorageReference creator, BiConsumer<StorageReference, StorageReference> handler) {
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