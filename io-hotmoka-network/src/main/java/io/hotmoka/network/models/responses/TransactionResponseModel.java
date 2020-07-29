package io.hotmoka.network.models.responses;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.*;

@Immutable
public abstract class TransactionResponseModel {

    /**
     * Builds the model of the given transaction response.
     *
     * @param response the response
     * @return the corresponding model
     */
    public static TransactionResponseModel from(TransactionResponse response) {

        if (response == null)
            throw new InternalFailureException("unexpected null request");
        else if (response instanceof GameteCreationTransactionResponse)
            return new GameteCreationTransactionResponseModel((GameteCreationTransactionResponse) response);
        else if (response instanceof InitializationTransactionResponse)
            return new InitializationTransactionResponseModel((InitializationTransactionResponse) response);
        else if (response instanceof JarStoreInitialTransactionResponse)
            return new JarStoreInitialTransactionResponseModel((JarStoreInitialTransactionResponse) response);
        else if (response instanceof JarStoreTransactionFailedResponse)
            return new JarStoreTransactionFailedResponseModel((JarStoreTransactionFailedResponse) response);
        else if (response instanceof JarStoreTransactionSuccessfulResponse)
            return new JarStoreTransactionSuccessfulResponseModel((JarStoreTransactionSuccessfulResponse) response);
        else if (response instanceof ConstructorCallTransactionFailedResponse)
            return new ConstructorCallTransactionFailedResponseModel((ConstructorCallTransactionFailedResponse) response);
        else if (response instanceof ConstructorCallTransactionSuccessfulResponse)
            return new ConstructorCallTransactionSuccessfulResponseModel((ConstructorCallTransactionSuccessfulResponse) response);
        else if (response instanceof ConstructorCallTransactionExceptionResponse)
            return new ConstuctorCallTransactionExceptionResponseModel((ConstructorCallTransactionExceptionResponse) response);
        else if (response instanceof MethodCallTransactionFailedResponse)
            return new MethodCallTransactionFailedResponseModel((MethodCallTransactionFailedResponse) response);
        else if (response instanceof MethodCallTransactionSuccessfulResponse)
            return new MethodCallTransactionSuccessfulResponseModel((MethodCallTransactionSuccessfulResponse) response);
        else if (response instanceof VoidMethodCallTransactionSuccessfulResponse)
            return new VoidMethodCallTransactionSuccessfulResponseModel((VoidMethodCallTransactionSuccessfulResponse) response);
        else if (response instanceof MethodCallTransactionExceptionResponse)
            return new MethodCallTransactionExceptionResponseModel((MethodCallTransactionExceptionResponse) response);
        else
            throw new InternalFailureException("unexpected transaction response of class " + response.getClass().getName());
    }

}
