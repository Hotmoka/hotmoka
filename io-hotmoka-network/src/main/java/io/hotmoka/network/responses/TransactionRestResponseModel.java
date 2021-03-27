package io.hotmoka.network.responses;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.InitializationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;

/**
 * Class which wraps a type response model
 * 
 * @param <T> the type response model
 */
public class TransactionRestResponseModel<T> {
    /**
     * The response model which should be an instance of {@link TransactionResponseModel}.
     */
    public T transactionResponseModel;

    /**
     * The runtime type of the response model
     */
    public String type;

    public TransactionRestResponseModel(T transactionResponseModel) {
        this.transactionResponseModel = transactionResponseModel;
        this.type = transactionResponseModel != null ? transactionResponseModel.getClass().getName() : null;
    }

    public TransactionRestResponseModel() {}

    /**
     * Builds the model of the given transaction response.
     *
     * @param response the response
     * @return the corresponding model
     */
    public static TransactionRestResponseModel<?> from(TransactionResponse response) {
        if (response == null)
            throw new InternalFailureException("unexpected null response");
        else if (response instanceof GameteCreationTransactionResponse)
            return new TransactionRestResponseModel<>(new GameteCreationTransactionResponseModel((GameteCreationTransactionResponse) response));
        else if (response instanceof InitializationTransactionResponse)
            return new TransactionRestResponseModel<>(new InitializationTransactionResponseModel());
        else if (response instanceof JarStoreInitialTransactionResponse)
            return new TransactionRestResponseModel<>(new JarStoreInitialTransactionResponseModel((JarStoreInitialTransactionResponse) response));
        else if (response instanceof JarStoreTransactionFailedResponse)
            return new TransactionRestResponseModel<>(new JarStoreTransactionFailedResponseModel((JarStoreTransactionFailedResponse) response));
        else if (response instanceof JarStoreTransactionSuccessfulResponse)
            return new TransactionRestResponseModel<>(new JarStoreTransactionSuccessfulResponseModel((JarStoreTransactionSuccessfulResponse) response));
        else if (response instanceof ConstructorCallTransactionFailedResponse)
            return new TransactionRestResponseModel<>(new ConstructorCallTransactionFailedResponseModel((ConstructorCallTransactionFailedResponse) response));
        else if (response instanceof ConstructorCallTransactionSuccessfulResponse)
            return new TransactionRestResponseModel<>(new ConstructorCallTransactionSuccessfulResponseModel((ConstructorCallTransactionSuccessfulResponse) response));
        else if (response instanceof ConstructorCallTransactionExceptionResponse)
            return new TransactionRestResponseModel<>(new ConstructorCallTransactionExceptionResponseModel((ConstructorCallTransactionExceptionResponse) response));
        else if (response instanceof MethodCallTransactionFailedResponse)
            return new TransactionRestResponseModel<>(new MethodCallTransactionFailedResponseModel((MethodCallTransactionFailedResponse) response));
        else if (response instanceof MethodCallTransactionSuccessfulResponse)
            return new TransactionRestResponseModel<>(new MethodCallTransactionSuccessfulResponseModel((MethodCallTransactionSuccessfulResponse) response));
        else if (response instanceof VoidMethodCallTransactionSuccessfulResponse)
            return new TransactionRestResponseModel<>(new VoidMethodCallTransactionSuccessfulResponseModel((VoidMethodCallTransactionSuccessfulResponse) response));
        else if (response instanceof MethodCallTransactionExceptionResponse)
            return new TransactionRestResponseModel<>(new MethodCallTransactionExceptionResponseModel((MethodCallTransactionExceptionResponse) response));
        else
            throw new InternalFailureException("unexpected transaction response of class " + response.getClass().getName());
    }
}
