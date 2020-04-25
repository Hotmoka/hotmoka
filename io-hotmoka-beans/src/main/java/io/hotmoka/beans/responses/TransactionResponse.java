package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * The response of a transaction.
 */
public interface TransactionResponse extends Serializable {

	/**
	 * Marshals this response into the given stream. This method
	 * in general performs better than standard Java serialization, wrt the size
	 * of the marshalled data.
	 * 
	 * @param oos the stream
	 * @throws IOException if the response cannot be marshalled
	 */
	void into(ObjectOutputStream oos) throws IOException;

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the response could not be unmarshalled
	 * @throws ClassNotFoundException if the response could not be unmarshalled
	 */
	static TransactionResponse from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte selector = ois.readByte();
		switch (selector) {
		case GameteCreationTransactionResponse.SELECTOR:
		case JarStoreInitialTransactionResponse.SELECTOR:
		case JarStoreTransactionFailedResponse.SELECTOR:
		case JarStoreTransactionSuccessfulResponse.SELECTOR:
		case ConstructorCallTransactionExceptionResponse.SELECTOR:
		case ConstructorCallTransactionFailedResponse.SELECTOR:
		case ConstructorCallTransactionSuccessfulResponse.SELECTOR:
		case MethodCallTransactionExceptionResponse.SELECTOR:
		case MethodCallTransactionFailedResponse.SELECTOR:
		case MethodCallTransactionSuccessfulResponse.SELECTOR:
		case VoidMethodCallTransactionSuccessfulResponse.SELECTOR:
		default: throw new IOException("unexpected response selector: " + selector);
		}
	}
}