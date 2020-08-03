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
            return new InitializationTransactionResponseModel();
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
            return new ConstructorCallTransactionExceptionResponseModel((ConstructorCallTransactionExceptionResponse) response);
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

    /**
     * Builds the transaction response of the given response model.
     *
     * @param responseModel the response model
     * @return the corresponding transaction response
     */
    public static TransactionResponse toBeanFrom(TransactionResponseModel responseModel) {
        if (responseModel == null)
            throw new InternalFailureException("unexpected null response model");
        else if (responseModel instanceof GameteCreationTransactionResponseModel)
            return ((GameteCreationTransactionResponseModel) responseModel).toBean();
        else if (responseModel instanceof InitializationTransactionResponseModel)
            return ((InitializationTransactionResponseModel) responseModel).toBean();
        else if (responseModel instanceof JarStoreInitialTransactionResponseModel)
            return ((JarStoreInitialTransactionResponseModel) responseModel).toBean();
        else if (responseModel instanceof JarStoreTransactionFailedResponseModel)
            return ((JarStoreTransactionFailedResponseModel) responseModel).toBean();
        else if (responseModel instanceof JarStoreTransactionSuccessfulResponseModel)
            return ((JarStoreTransactionSuccessfulResponseModel) responseModel).toBean();
        else if (responseModel instanceof ConstructorCallTransactionFailedResponseModel)
            return ((ConstructorCallTransactionFailedResponseModel) responseModel).toBean();
        else if (responseModel instanceof ConstructorCallTransactionSuccessfulResponseModel)
            return ((ConstructorCallTransactionSuccessfulResponseModel) responseModel).toBean();
        else if (responseModel instanceof ConstructorCallTransactionExceptionResponseModel)
            return ((ConstructorCallTransactionExceptionResponseModel) responseModel).toBean();
        else if (responseModel instanceof MethodCallTransactionFailedResponseModel)
            return ((MethodCallTransactionFailedResponseModel) responseModel).toBean();
        else if (responseModel instanceof MethodCallTransactionSuccessfulResponseModel)
            return ((MethodCallTransactionSuccessfulResponseModel) responseModel).toBean();
        else if (responseModel instanceof VoidMethodCallTransactionSuccessfulResponseModel)
            return ((VoidMethodCallTransactionSuccessfulResponseModel) responseModel).toBean();
        else if (responseModel instanceof MethodCallTransactionExceptionResponseModel)
            return ((MethodCallTransactionExceptionResponseModel) responseModel).toBean();
        else
            throw new InternalFailureException("unexpected transaction response model of class " + responseModel.getClass().getName());
    }
}