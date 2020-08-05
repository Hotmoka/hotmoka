package io.hotmoka.network.models.responses;

import com.google.gson.Gson;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.*;

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

        if (restResponseModel.getType() == null)
            throw new InternalFailureException("unexpected null rest response type model");

        if (restResponseModel.transactionResponseModel == null)
            throw new InternalFailureException("unexpected null rest response object model");

        Gson gson = new Gson();
        String serialized = serialize(gson, restResponseModel);

        if (serialized == null)
            throw new InternalFailureException("unexpected null serialized object");
        else if (restResponseModel.getType().equals(JarStoreInitialTransactionResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreInitialTransactionResponseModel.class).toBean();
        else if (restResponseModel.getType().equals(GameteCreationTransactionResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreInitialTransactionResponseModel.class).toBean();
        else if (restResponseModel.getType().equals(InitializationTransactionResponseModel.class.getName()))
            return gson.fromJson(serialized, InitializationTransactionResponseModel.class).toBean();
        else if (restResponseModel.getType().equals(JarStoreTransactionFailedResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreTransactionFailedResponseModel.class).toBean();
        else if (restResponseModel.getType().equals(JarStoreTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.getType().equals(ConstructorCallTransactionFailedResponseModel.class.getName()))
            return gson.fromJson(serialized, ConstructorCallTransactionFailedResponseModel.class).toBean();
        else if (restResponseModel.getType().equals(ConstructorCallTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, ConstructorCallTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.getType().equals(ConstructorCallTransactionExceptionResponseModel.class.getName()))
            return gson.fromJson(serialized, ConstructorCallTransactionExceptionResponseModel.class).toBean();
        else if (restResponseModel.getType().equals(MethodCallTransactionFailedResponseModel.class.getName()))
            return gson.fromJson(serialized, MethodCallTransactionFailedResponseModel.class).toBean();
        else if (restResponseModel.getType().equals(MethodCallTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, MethodCallTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.getType().equals(VoidMethodCallTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, VoidMethodCallTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.getType().equals(MethodCallTransactionExceptionResponseModel.class.getName()))
            return gson.fromJson(serialized, MethodCallTransactionExceptionResponseModel.class).toBean();
        else
            throw new InternalFailureException("unexpected transaction rest response model of class " + restResponseModel.getType());
    }

    /**
     * Serializes the transaction response model of the rest model
     * @param gson the gson instance
     * @param restResponseModel the rest model
     * @return the string
     */
    private static String serialize(Gson gson, TransactionRestResponseModel<?> restResponseModel) {

        try {
            return gson.toJson(restResponseModel.transactionResponseModel);
        }
        catch (Exception e) {
            throw new InternalFailureException("unexpected serialization error");
        }
    }

}