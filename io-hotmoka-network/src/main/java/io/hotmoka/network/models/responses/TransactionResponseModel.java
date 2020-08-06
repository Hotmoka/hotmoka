package io.hotmoka.network.models.responses;

import com.google.gson.Gson;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.TransactionResponse;

@Immutable
public abstract class TransactionResponseModel {


    /**
     * Builds the transaction response for the given rest response model.
     *
     * @param restResponseModel the rest response model
     * @return the corresponding transaction response
     */
    public static TransactionResponse toBeanFrom(TransactionRestResponseModel<?> restResponseModel) {
        if (restResponseModel == null)
            throw new InternalFailureException("unexpected null rest response model");

        if (restResponseModel.type == null)
            throw new InternalFailureException("unexpected null rest response type model");

        if (restResponseModel.transactionResponseModel == null)
            throw new InternalFailureException("unexpected null rest response object model");

        final Gson gson = new Gson();
        final String serialized = serialize(gson, restResponseModel);

        if (serialized == null)
            throw new InternalFailureException("unexpected null serialized object");
        else if (restResponseModel.type.equals(JarStoreInitialTransactionResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreInitialTransactionResponseModel.class).toBean();
        else if (restResponseModel.type.equals(GameteCreationTransactionResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreInitialTransactionResponseModel.class).toBean();
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

}