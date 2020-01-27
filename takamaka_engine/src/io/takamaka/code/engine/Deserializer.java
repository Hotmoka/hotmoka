package io.takamaka.code.engine;

import io.hotmoka.beans.values.StorageValue;

/**
 * An object that translates storage values into RAM values.
 */
public interface Deserializer {

	/**
	 * Yields the deserialization of the given value. That is, it yields an actual object in RAM
	 * that reflects its representation in blockchain.
	 * 
	 * @param value the value to deserialize
	 * @return the deserialization of {@code value}
	 * @throws DeserializationError if the value cannot be deserialized
	 */
	Object deserialize(StorageValue value) throws DeserializationError;
}