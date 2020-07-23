package io.hotmoka.network.json;

import java.util.Base64;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;

/**
 * A collection of methods that transform requests into their JSON representation.
 */
public class JSONTransactionRequest {

	public static String from(ConstructorCallTransactionRequest request) {
		//TODO
		JsonObject bodyJson = new JsonObject();
		//bodyJson.addProperty("classpath", classpath);
		bodyJson.addProperty("signature", Base64.getEncoder().encodeToString(request.getSignature()));

		JsonArray values = new JsonArray();
		//values.add(buildValue("int", "1973"));

		return null;
	}
}