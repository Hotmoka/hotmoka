package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.TransactionResponse;

/**
 * A request of a transaction.
 * 
 * @param <R> the type of the response expected for this request
 */
@Immutable
public abstract class TransactionRequest<R extends TransactionResponse> extends Marshallable {

	/**
	 * Used to marshal requests that are specific to a node.
	 * After this selector, the qualified name of the request must follow.
	 */
	protected final static byte EXPANSION_SELECTOR = 11;

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static TransactionRequest<?> from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte selector = ois.readByte();
		switch (selector) {
		case ConstructorCallTransactionRequest.SELECTOR: return ConstructorCallTransactionRequest.from(ois);
		case GameteCreationTransactionRequest.SELECTOR: return GameteCreationTransactionRequest.from(ois);
		case InitializationTransactionRequest.SELECTOR: return InitializationTransactionRequest.from(ois);
		case InstanceMethodCallTransactionRequest.SELECTOR:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_INT:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_LONG:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_BIG_INTEGER:
			return InstanceMethodCallTransactionRequest.from(ois, selector);
		case JarStoreInitialTransactionRequest.SELECTOR: return JarStoreInitialTransactionRequest.from(ois);
		case JarStoreTransactionRequest.SELECTOR: return JarStoreTransactionRequest.from(ois);
		case RedGreenGameteCreationTransactionRequest.SELECTOR: return RedGreenGameteCreationTransactionRequest.from(ois);
		case StaticMethodCallTransactionRequest.SELECTOR: return StaticMethodCallTransactionRequest.from(ois);
		case EXPANSION_SELECTOR: {
			// this case deals with requests that only exist in a specific type of node;
			// hence their fully-qualified name must be available after the expansion selector

			String className = ois.readUTF();
			Class<?> clazz = Class.forName(className, false, ClassLoader.getSystemClassLoader());

			// only subclass of TransactionRequest are considered, to block potential call injections
			if (!TransactionRequest.class.isAssignableFrom(clazz))
				throw new IOException("unknown request class " + className);

			Method from;
			try {
				from = clazz.getMethod("from", ObjectInputStream.class);
			}
			catch (NoSuchMethodException | SecurityException e) {
				throw new IOException("cannot find method " + className + ".from(ObjectInputStream)");
			}

			try {
				return (TransactionRequest<?>) from.invoke(null, ois);
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IOException("cannot call method " + className + ".from(ObjectInputStream)");
			}
		}
		default: throw new IOException("unexpected request selector: " + selector);
		}
	}

	/**
	 * Unmarshals the signature from the given stream.
	 * 
	 * @param ois the stream
	 * @return the signature
	 * @throws IOException if the signature could not be unmarshalled
	 */
	protected final static byte[] unmarshallSignature(ObjectInputStream ois) throws IOException {
		int signatureLength = readLength(ois);
		byte[] signature = new byte[signatureLength];
		if (signatureLength != ois.readNBytes(signature, 0, signatureLength))
			throw new IOException("signature length mismatch in request");

		return signature;
	}
}