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

package io.hotmoka.node.remote.internal;

import static io.hotmoka.node.service.api.NodeService.GET_NODE_INFO_ENDPOINT;

import java.io.IOException;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.nodes.NodeInfo;
import io.hotmoka.beans.api.requests.MethodCallTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.signatures.VoidMethodSignature;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.network.NetworkExceptionResponse;
import io.hotmoka.network.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.requests.EventRequestModel;
import io.hotmoka.network.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.network.requests.InitializationTransactionRequestModel;
import io.hotmoka.network.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.requests.TransactionRestRequestModel;
import io.hotmoka.network.responses.ConstructorCallTransactionExceptionResponseModel;
import io.hotmoka.network.responses.ConstructorCallTransactionFailedResponseModel;
import io.hotmoka.network.responses.ConstructorCallTransactionSuccessfulResponseModel;
import io.hotmoka.network.responses.GameteCreationTransactionResponseModel;
import io.hotmoka.network.responses.InitializationTransactionResponseModel;
import io.hotmoka.network.responses.JarStoreInitialTransactionResponseModel;
import io.hotmoka.network.responses.JarStoreTransactionFailedResponseModel;
import io.hotmoka.network.responses.JarStoreTransactionSuccessfulResponseModel;
import io.hotmoka.network.responses.MethodCallTransactionExceptionResponseModel;
import io.hotmoka.network.responses.MethodCallTransactionFailedResponseModel;
import io.hotmoka.network.responses.MethodCallTransactionSuccessfulResponseModel;
import io.hotmoka.network.responses.TransactionRestResponseModel;
import io.hotmoka.network.responses.VoidMethodCallTransactionSuccessfulResponseModel;
import io.hotmoka.network.values.StorageValueModel;
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.SubscriptionsManagers;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.SubscriptionsManager;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.messages.GetNodeInfoMessages;
import io.hotmoka.node.messages.GetNodeInfoResultMessages;
import io.hotmoka.node.messages.api.GetNodeInfoMessage;
import io.hotmoka.node.messages.api.GetNodeInfoResultMessage;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.node.remote.api.RemoteNodeConfig;
import io.hotmoka.node.remote.internal.websockets.client.WebSocketClient;
import io.hotmoka.websockets.beans.ExceptionMessages;
import io.hotmoka.websockets.beans.api.ExceptionMessage;
import io.hotmoka.websockets.beans.api.RpcMessage;
import io.hotmoka.websockets.client.AbstractRemote;
import io.hotmoka.ws.client.WebSocketException;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Session;

/**
 * Shared implementation of a node that forwards all its calls to a remote service.
 */
@ThreadSafe
public abstract class AbstractRemoteNode extends AbstractRemote<NodeException> implements RemoteNode {

    /**
     * The configuration of the node.
     */
    protected final RemoteNodeConfig config;

    /**
     * The websocket client for the remote node, one per thread.
     */
    protected final WebSocketClient webSocketClient;

	/**
	 * The manager of the subscriptions to the events occurring in this node.
	 */
	private final SubscriptionsManager subscriptions = SubscriptionsManagers.mk();

	/**
	 * The prefix used in the log messages;
	 */
	private final String logPrefix;

	private final static Logger LOGGER = Logger.getLogger(AbstractRemoteNode.class.getName());

	/**
     * Builds the remote node.
     *
     * @param config the configuration of the node
	 * @throws DeploymentException if the remote node could not be deployed
	 * @throws IOException if the remote node could not be created
     */
    protected AbstractRemoteNode(RemoteNodeConfig config) throws IOException, DeploymentException {
    	super(10_000L); // TODO: this should be contained in the config

    	String modifiedURL = config.getURL().substring(0, config.getURL().length() - 1); // TODO: remove this +2 at the end
    	modifiedURL += (char) (config.getURL().charAt(config.getURL().length() - 1) + 2);
    	URI uri = URI.create("ws://" + modifiedURL); // TODO: the URI should already be in the config
    	this.config = config;
    	this.logPrefix = "node remote(ws://" + config.getURL() + "): "; // TODO: just uri at the end

    	addSession(GET_NODE_INFO_ENDPOINT, uri, GetNodeInfoEndpoint::new);

    	try {
        	this.webSocketClient = new WebSocketClient("ws://" + config.getURL() + "/node");
        }
        catch (WebSocketException e) {
        	throw new IOException(e);
        }
        catch (InterruptedException | ExecutionException e) {
        	throw new RuntimeException("unexpected exception", e);
        }

        subscribeToEventsTopic();
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
		webSocketClient.close();
		LOGGER.info(logPrefix + "closed with reason: " + reason);
	}

	@Override
	protected void notifyResult(RpcMessage message) {
		if (message instanceof GetNodeInfoResultMessage gnirm)
			onGetNodeInfoResult(gnirm);
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
	public final Subscription subscribeToEvents(StorageReference creator, BiConsumer<StorageReference, StorageReference> handler) {
		return subscriptions.subscribeToEvents(creator, handler);
	}

	/**
	 * Notifies the given event to all event handlers for the given creator.
	 * 
	 * @param creator the creator of the event
	 * @param event the event to notify
	 */
	protected final void notifyEvent(StorageReference creator, StorageReference event) {
		subscriptions.notifyEvent(creator, event);
		LOGGER.info(logPrefix + event + ": notified as event with creator " + creator);
	}

	/**
     * Subscribes to the events topic of the remote node to get notified about the node events.
     */
    private void subscribeToEventsTopic() {
        webSocketClient.subscribeToTopic("/topic/events", EventRequestModel.class, (eventRequestModel, errorModel) -> {
            if (eventRequestModel != null)
                notifyEvent(eventRequestModel.creator.toBean(), eventRequestModel.event.toBean());
            else
                LOGGER.info(logPrefix + "got error from event subscription: " + errorModel.exceptionClassName + ": " + errorModel.message);
        });
    }

    /**
     * Deals with methods that return void: the API of the node
     * requires to return null, always, when such methods are called.
     *
     * @param request the request that calls the method
     * @param model the model of the return value of the method
     * @return the resulting value, using {@code null} if the method returned void
     */
    protected static StorageValue dealWithReturnVoid(MethodCallTransactionRequest request, StorageValueModel model) {
        return request.getStaticTarget() instanceof VoidMethodSignature ? null : model.toBean();
    }

    /**
     * Build the transaction request from the given model.
     *
     * @param restRequestModel the request model
     * @return the corresponding transaction request
     */
    protected static TransactionRequest<?> requestFromModel(TransactionRestRequestModel<?> restRequestModel) {
        if (restRequestModel == null)
            throw new RuntimeException("unexpected null rest request model");

        if (restRequestModel.type == null)
            throw new RuntimeException("unexpected null rest request type model");

        if (restRequestModel.transactionRequestModel == null)
            throw new RuntimeException("unexpected null rest request object model");

        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        final String serialized = serialize(gson, restRequestModel);

        if (serialized == null)
            throw new RuntimeException("unexpected null serialized object");
        if (restRequestModel.type.equals(ConstructorCallTransactionRequestModel.class.getName()))
            return gson.fromJson(serialized, ConstructorCallTransactionRequestModel.class).toBean();
        else if (restRequestModel.type.equals(InitializationTransactionRequestModel.class.getName()))
            return gson.fromJson(serialized, InitializationTransactionRequestModel.class).toBean();
        else if (restRequestModel.type.equals(InstanceMethodCallTransactionRequestModel.class.getName()))
            return gson.fromJson(serialized, InstanceMethodCallTransactionRequestModel.class).toBean();
        else if (restRequestModel.type.equals(JarStoreInitialTransactionRequestModel.class.getName()))
            return gson.fromJson(serialized, JarStoreInitialTransactionRequestModel.class).toBean();
        else if (restRequestModel.type.equals(JarStoreTransactionRequestModel.class.getName()))
            return gson.fromJson(serialized, JarStoreTransactionRequestModel.class).toBean();
        else if (restRequestModel.type.equals(GameteCreationTransactionRequestModel.class.getName()))
            return gson.fromJson(serialized, GameteCreationTransactionRequestModel.class).toBean();
        else if (restRequestModel.type.equals(StaticMethodCallTransactionRequestModel.class.getName()))
            return gson.fromJson(serialized, StaticMethodCallTransactionRequestModel.class).toBean();
        else
            throw new RuntimeException("unexpected transaction request model of class " + restRequestModel.type);
    }

    /**
     * Serializes the transaction request model of the rest model
     * @param gson the gson instance
     * @param restRequestModel the rest model
     * @return the string
     */
    private static String serialize(Gson gson, TransactionRestRequestModel<?> restRequestModel) {
    	return gson.toJsonTree(restRequestModel.transactionRequestModel).toString();
    }

    /**
     * Builds the transaction response for the given rest response model.
     *
     * @param restResponseModel the rest response model
     * @return the corresponding transaction response
     */
    protected static TransactionResponse responseFromModel(TransactionRestResponseModel<?> restResponseModel) {
        if (restResponseModel == null)
            throw new RuntimeException("unexpected null rest response model");

        if (restResponseModel.type == null)
            throw new RuntimeException("unexpected null rest response type model");

        if (restResponseModel.transactionResponseModel == null)
            throw new RuntimeException("unexpected null rest response object model");

        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        final String serialized = serialize(gson, restResponseModel);

        if (serialized == null)
            throw new RuntimeException("unexpected null serialized object");
        else if (restResponseModel.type.equals(JarStoreInitialTransactionResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreInitialTransactionResponseModel.class).toBean();
        else if (restResponseModel.type.equals(GameteCreationTransactionResponseModel.class.getName()))
            return gson.fromJson(serialized, GameteCreationTransactionResponseModel.class).toBean();
        else if (restResponseModel.type.equals(InitializationTransactionResponseModel.class.getName()))
            return gson.fromJson(serialized, InitializationTransactionResponseModel.class).toBean();
        else if (restResponseModel.type.equals(JarStoreTransactionFailedResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreTransactionFailedResponseModel.class).toBean();
        else if (restResponseModel.type.equals(JarStoreTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.type.equals(ConstructorCallTransactionFailedResponseModel.class.getName()))
            return gson.fromJson(serialized, ConstructorCallTransactionFailedResponseModel.class).toBean();
        else if (restResponseModel.type.equals(ConstructorCallTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, ConstructorCallTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.type.equals(ConstructorCallTransactionExceptionResponseModel.class.getName()))
            return gson.fromJson(serialized, ConstructorCallTransactionExceptionResponseModel.class).toBean();
        else if (restResponseModel.type.equals(MethodCallTransactionFailedResponseModel.class.getName()))
            return gson.fromJson(serialized, MethodCallTransactionFailedResponseModel.class).toBean();
        else if (restResponseModel.type.equals(MethodCallTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, MethodCallTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.type.equals(VoidMethodCallTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, VoidMethodCallTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.type.equals(MethodCallTransactionExceptionResponseModel.class.getName()))
            return gson.fromJson(serialized, MethodCallTransactionExceptionResponseModel.class).toBean();
        else
            throw new RuntimeException("unexpected transaction rest response model of class " + restResponseModel.type);
    }

    /**
     * Serializes the transaction response model of the rest model
     * @param gson the gson instance
     * @param restResponseModel the rest model
     * @return the string
     */
    private static String serialize(Gson gson, TransactionRestResponseModel<?> restResponseModel) {
    	return gson.toJsonTree(restResponseModel.transactionResponseModel).toString();
    }

	/**
     * Runs a callable and wraps the exception by its type.
     * If the type doesn't match any of the methods signature type then
     * it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
     *
     * @param <T> the return type of the callable
     * @param what the callable
     * @return the return value of the callable
     * @throws TransactionRejectedException the wrapped exception
     * @throws TransactionException the wrapped exception
     * @throws CodeExecutionException the wrapped exception
     */
    protected static <T> T wrapNetworkExceptionFull(Callable<T> what) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        try {
            return what.call();
        }
        catch (NetworkExceptionResponse e) {
            if (e.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
                throw new TransactionRejectedException(e.getMessage());
            else if (e.getExceptionClassName().equals(TransactionException.class.getName()))
                throw new TransactionException(e.getMessage());
            else if (e.getExceptionClassName().equals(CodeExecutionException.class.getName()))
                throw new CodeExecutionException(e.getMessage());
            else
                throw new RuntimeException(e.getMessage());
        }
        catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw e;
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs a callable and wraps the exception by its type.
     * If the type doesn't match any of the methods signature type then
     * it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
     *
     * @param <T> the return type of the callable
     * @param what the callable
     * @return the return value of the callable
     * @throws TransactionRejectedException the wrapped exception
     * @throws TransactionException the wrapped exception
     */
    protected static <T> T wrapNetworkExceptionMedium(Callable<T> what) throws TransactionRejectedException, TransactionException {
        try {
            return what.call();
        }
        catch (NetworkExceptionResponse e) {
            if (e.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
                throw new TransactionRejectedException(e.getMessage());
            else if (e.getExceptionClassName().equals(TransactionException.class.getName()))
                throw new TransactionException(e.getMessage());
            else
                throw new RuntimeException(e.getMessage());
        }
        catch (RuntimeException e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw e;
        }
        catch (Exception e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs a callable and wraps the exception by its type.
     * If the type doesn't match {@link io.hotmoka.node.api.TransactionRejectedException} then it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
     *
     * @param <T> the return type of the callable
     * @param what the callable
     * @return the return value of the callable
     * @throws TransactionRejectedException the wrapped exception
     */
    protected static <T> T wrapNetworkExceptionSimple(Callable<T> what) throws TransactionRejectedException {
        try {
            return what.call();
        }
        catch (NetworkExceptionResponse e) {
            if (e.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
                throw new TransactionRejectedException(e.getMessage());
            else
                throw new RuntimeException(e.getMessage());
        }
        catch (RuntimeException e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw e;
        }
        catch (Exception e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs a callable and wraps the exception by its type.
     * If the type doesn't match {@link java.util.NoSuchElementException} then it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
     *
     * @param <T> the return type of the callable
     * @param what the callable
     * @return the return value of the callable
     * @throws NoSuchElementException the wrapped exception
     */
    protected static <T> T wrapNetworkExceptionForNoSuchElementException(Callable<T> what) throws NoSuchElementException {
        try {
            return what.call();
        }
        catch (NetworkExceptionResponse e) {
            if (e.getExceptionClassName().equals(NoSuchElementException.class.getName()))
                throw new NoSuchElementException(e.getMessage());
            else
                throw new RuntimeException(e.getMessage());
        }
        catch (RuntimeException e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw e;
        }
        catch (Exception e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs a callable and wraps the exception by its type.
     *
     * @param <T> the return type of the callable
     * @param what the callable
     * @return the return value of the callable
     */
    protected static <T> T wrapNetworkExceptionBasic(Callable<T> what) {
        try {
            return what.call();
        }
        catch (NetworkExceptionResponse exceptionResponse) {
            throw new RuntimeException(exceptionResponse.getMessage());
        }
        catch (RuntimeException e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw e;
        }
        catch (Exception e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs a callable and wraps the exception by its type.
     * If the type doesn't match any of the methods signature type then
     * it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
     *
     * @param <T> the return type of the callable
     * @param what the callable
     * @return the return value of the callable
     * @throws TransactionRejectedException the wrapped exception
     * @throws TimeoutException the wrapped exception
     * @throws InterruptedException the wrapped exception
     */
    protected static <T> T wrapNetworkExceptionForPolledResponseException(Callable<T> what) throws TransactionRejectedException, TimeoutException, InterruptedException  {
        try {
            return what.call();
        }
        catch (NetworkExceptionResponse exceptionResponse) {
            if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
                throw new TransactionRejectedException(exceptionResponse.getMessage());
            else if (exceptionResponse.getExceptionClassName().equals(TimeoutException.class.getName()))
                throw new TimeoutException(exceptionResponse.getMessage());
            else if (exceptionResponse.getExceptionClassName().equals(InterruptedException.class.getName()))
                throw new InterruptedException(exceptionResponse.getMessage());
            else
                throw new RuntimeException(exceptionResponse.getMessage());
        }
        catch (RuntimeException e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw e;
        }
        catch (Exception e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs a callable and wraps the exception by its type.
     * If the type doesn't match any of the methods signature type then
     * it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
     *
     * @param <T> the return type of the callable
     * @param what the callable
     * @return the return value of the callable
     * @throws TransactionRejectedException the wrapped exception
     * @throws NoSuchElementException the wrapped exception
     */
    protected static <T> T wrapNetworkExceptionForResponseAtException(Callable<T> what) throws TransactionRejectedException, NoSuchElementException {
        try {
            return what.call();
        }
        catch (NetworkExceptionResponse exceptionResponse) {
            if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
                throw new TransactionRejectedException(exceptionResponse.getMessage());
            else if (exceptionResponse.getExceptionClassName().equals(NoSuchElementException.class.getName()))
                throw new NoSuchElementException(exceptionResponse.getMessage());
            else
                throw new RuntimeException(exceptionResponse.getMessage());
        }
        catch (RuntimeException e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw e;
        }
        catch (Exception e) {
        	LOGGER.log(Level.WARNING, "unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
	 * Runs a callable and wraps any exception into an {@link TransactionRejectedException}.
	 * 
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 */
	protected static <T> T wrapInCaseOfExceptionSimple(Callable<T> what) throws TransactionRejectedException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException e) {
			throw e;
		}
		catch (Throwable t) {
			LOGGER.log(Level.WARNING, "unexpected exception", t);
			throw new TransactionRejectedException(t);
		}
	}
}