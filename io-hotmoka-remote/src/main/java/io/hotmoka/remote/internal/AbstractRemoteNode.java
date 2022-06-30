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

package io.hotmoka.remote.internal;

import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.requests.MethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.StorageValue;
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
import io.hotmoka.nodes.AbstractNode;
import io.hotmoka.remote.RemoteNode;
import io.hotmoka.remote.RemoteNodeConfig;
import io.hotmoka.remote.internal.websockets.client.WebSocketClient;

/**
 * Shared implementation of a node that forwards all its calls to a remote service.
 */
@ThreadSafe
public abstract class AbstractRemoteNode extends AbstractNode implements RemoteNode {

    /**
     * The configuration of the node.
     */
    protected final RemoteNodeConfig config;

    /**
     * The websocket client for the remote node, one per thread.
     */
    protected final WebSocketClient webSocketClient;

    /**
     * Builds the remote node.
     *
     * @param config the configuration of the node
     */
    protected AbstractRemoteNode(RemoteNodeConfig config) {
        this.config = config;

        try {
            webSocketClient = new WebSocketClient("ws://" + config.url + "/node");
        }
        catch (Exception e) {
            throw InternalFailureException.of(e);
        }

        subscribeToEventsTopic();
    }

    /**
     * Subscribes to the events topic of the remote node to get notified about the node events.
     */
    private void subscribeToEventsTopic() {
        webSocketClient.subscribeToTopic("/topic/events", EventRequestModel.class, (eventRequestModel, errorModel) -> {
            if (eventRequestModel != null)
                notifyEvent(eventRequestModel.creator.toBean(), eventRequestModel.event.toBean());
            else
                logger.info("Got error from event subscription: " + errorModel.exceptionClassName + ": " + errorModel.message);
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
            throw new InternalFailureException("unexpected null rest request model");

        if (restRequestModel.type == null)
            throw new InternalFailureException("unexpected null rest request type model");

        if (restRequestModel.transactionRequestModel == null)
            throw new InternalFailureException("unexpected null rest request object model");

        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        final String serialized = serialize(gson, restRequestModel);

        if (serialized == null)
            throw new InternalFailureException("unexpected null serialized object");
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
            throw new InternalFailureException("unexpected transaction request model of class " + restRequestModel.type);
    }

    /**
     * Serializes the transaction request model of the rest model
     * @param gson the gson instance
     * @param restRequestModel the rest model
     * @return the string
     */
    private static String serialize(Gson gson, TransactionRestRequestModel<?> restRequestModel) {
        try {
            return gson.toJsonTree(restRequestModel.transactionRequestModel).toString();
        }
        catch (Exception e) {
            throw new InternalFailureException("unexpected serialization error");
        }
    }

    /**
     * Builds the transaction response for the given rest response model.
     *
     * @param restResponseModel the rest response model
     * @return the corresponding transaction response
     */
    protected static TransactionResponse responseFromModel(TransactionRestResponseModel<?> restResponseModel) {
        if (restResponseModel == null)
            throw new InternalFailureException("unexpected null rest response model");

        if (restResponseModel.type == null)
            throw new InternalFailureException("unexpected null rest response type model");

        if (restResponseModel.transactionResponseModel == null)
            throw new InternalFailureException("unexpected null rest response object model");

        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        final String serialized = serialize(gson, restResponseModel);

        if (serialized == null)
            throw new InternalFailureException("unexpected null serialized object");
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
            throw new InternalFailureException("unexpected transaction rest response model of class " + restResponseModel.type);
    }

    /**
     * Serializes the transaction response model of the rest model
     * @param gson the gson instance
     * @param restResponseModel the rest model
     * @return the string
     */
    private static String serialize(Gson gson, TransactionRestResponseModel<?> restResponseModel) {
        try {
            return gson.toJsonTree(restResponseModel.transactionResponseModel).toString();
        }
        catch (Exception e) {
            throw new InternalFailureException("unexpected serialization error");
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
     * @throws CodeExecutionException the wrapped exception
     */
    protected static <T> T wrapNetworkExceptionFull(Callable<T> what) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        try {
            return what.call();
        }
        catch (NetworkExceptionResponse exceptionResponse) {
            if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
                throw new TransactionRejectedException(exceptionResponse.getMessage());
            else if (exceptionResponse.getExceptionClassName().equals(TransactionException.class.getName()))
                throw new TransactionException(exceptionResponse.getMessage());
            else if (exceptionResponse.getExceptionClassName().equals(CodeExecutionException.class.getName()))
                throw new CodeExecutionException(exceptionResponse.getMessage());
            else
                throw new InternalFailureException(exceptionResponse.getMessage());
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "unexpected error", e);
            throw new InternalFailureException(e.getMessage());
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
        catch (NetworkExceptionResponse exceptionResponse) {
            if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
                throw new TransactionRejectedException(exceptionResponse.getMessage());
            else if (exceptionResponse.getExceptionClassName().equals(TransactionException.class.getName()))
                throw new TransactionException(exceptionResponse.getMessage());
            else
                throw new InternalFailureException(exceptionResponse.getMessage());
        }
        catch (Exception e) {
        	logger.log(Level.WARNING, "unexpected error", e);
            throw new InternalFailureException(e.getMessage());
        }
    }

    /**
     * Runs a callable and wraps the exception by its type.
     * If the type doesn't match {@link io.hotmoka.beans.TransactionRejectedException} then it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
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
        catch (NetworkExceptionResponse exceptionResponse) {
            if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
                throw new TransactionRejectedException(exceptionResponse.getMessage());
            else
                throw new InternalFailureException(exceptionResponse.getMessage());
        }
        catch (Exception e) {
        	logger.log(Level.WARNING, "unexpected error", e);
            throw new InternalFailureException(e.getMessage());
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
        catch (NetworkExceptionResponse exceptionResponse) {
            if (exceptionResponse.getExceptionClassName().equals(NoSuchElementException.class.getName()))
                throw new NoSuchElementException(exceptionResponse.getMessage());
            else
                throw new InternalFailureException(exceptionResponse.getMessage());
        }
        catch (Exception e) {
        	logger.log(Level.WARNING, "unexpected error", e);
            throw new InternalFailureException(e.getMessage());
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
            throw new InternalFailureException(exceptionResponse.getMessage());
        }
        catch (Exception e) {
        	logger.log(Level.WARNING, "unexpected error", e);
            throw new InternalFailureException(e.getMessage());
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
                throw new InternalFailureException(exceptionResponse.getMessage());
        }
        catch (Exception e) {
        	logger.log(Level.WARNING, "unexpected error", e);
            throw new InternalFailureException(e.getMessage());
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
                throw new InternalFailureException(exceptionResponse.getMessage());
        }
        catch (Exception e) {
        	logger.log(Level.WARNING, "unexpected error", e);
            throw new InternalFailureException(e.getMessage());
        }
    }

    @Override
    public void close() {
        webSocketClient.close();
    }
}