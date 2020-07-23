package io.hotmoka.network.json;

import com.google.gson.JsonObject;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.StorageReference;

/**
 * A collection of methods that transform storage references into their JSON representation and back.
 */
public class JSONStorageReference {

	/**
	 * Yields the JSON corresponding to the given object.
	 * 
	 * @param reference the object to transform in its JSON representation
	 * @return the JSON representation of {@code reference}
	 */
	public static String intoJSON(TransactionReference reference) {
		if (reference instanceof LocalTransactionReference)
			return reference.getHash();
		else
			throw new InternalFailureException("unexpected transaction reference of class " + reference.getClass().getName());
	}

	public static StorageReference fromJSON(JsonObject json) {
		return null; // TODO
	}
}