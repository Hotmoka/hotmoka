package io.hotmoka.network.models.responses;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.*;

/**
 * Class which wraps a type response model
 * @param <T> the type response model
 */
@Immutable
public class TransactionRestResponseModel<T> {
    /**
     * The response model which should be an instance of {@link io.hotmoka.network.models.responses.TransactionResponseModel}.
     */
    public final T transactionResponseModel;

    /**
     * The runtime type of the response model
     */
    public final String type;

    public TransactionRestResponseModel(T transactionResponseModel) {
        this.transactionResponseModel = transactionResponseModel;
        this.type = transactionResponseModel != null ? transactionResponseModel.getClass().getName() : null;
    }

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
