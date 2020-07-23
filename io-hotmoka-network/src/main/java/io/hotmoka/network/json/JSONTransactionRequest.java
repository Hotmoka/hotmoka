package io.hotmoka.network.json;

import java.util.Base64;

import com.google.gson.JsonObject;

import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;

/**
 * A collection of methods that transform requests into their JSON representation and back.
 */
public class JSONTransactionRequest {

	/**
	 * Yields the JSON request corresponding to the given object.
	 * 
	 * @param request the object to transform in its JSON representation
	 * @return the JSON representation of {@code request}
	 */
	public static String toJSON(ConstructorCallTransactionRequest request) {
		//TODO
		JsonObject bodyJson = new JsonObject();
		//bodyJson.addProperty("classpath", classpath);
		bodyJson.addProperty("signature", Base64.getEncoder().encodeToString(request.getSignature()));

		//JsonArray values = new JsonArray();
		//values.add(buildValue("int", "1973"));

		return null;
	}

	public static ConstructorCallTransactionRequest fromJSON_ConstructorCallTransactionRequest(String json) {
		return null;
	}
}