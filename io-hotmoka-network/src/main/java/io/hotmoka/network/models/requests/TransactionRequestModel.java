package io.hotmoka.network.models.requests;

import com.google.gson.Gson;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.TransactionRequest;

import java.util.Base64;

@Immutable
public abstract class TransactionRequestModel {
    protected final byte[] decodeBase64(String what) {
    	return Base64.getDecoder().decode(what);
    }


	/**
	 * Build the transaction request from the given model.
	 * @param restRequestModel the request model
	 * @return the corresponding transaction request
	 */
	public static TransactionRequest<?> toBeanFrom(TransactionRestRequestModel<?> restRequestModel) {

		if (restRequestModel == null)
			throw new InternalFailureException("unexpected null rest request model");

		if (restRequestModel.type == null)
			throw new InternalFailureException("unexpected null rest request type model");

		if (restRequestModel.transactionRequestModel == null)
			throw new InternalFailureException("unexpected null rest request object model");

		Gson gson = new Gson();
		String serialized = serialize(gson, restRequestModel);

		if (restRequestModel.type.equals(ConstructorCallTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, ConstructorCallTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(GameteCreationTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, GameteCreationTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(InitializationTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, InitializationTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(InstanceMethodCallTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, InstanceMethodCallTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(JarStoreInitialTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, JarStoreInitialTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(JarStoreTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, JarStoreTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(RedGreenGameteCreationTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, RedGreenGameteCreationTransactionRequestModel.class).toBean();
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
			return gson.toJson(restRequestModel.transactionRequestModel);
		}
		catch (Exception e) {
			throw new InternalFailureException("unexpected serialization error");
		}
	}
}