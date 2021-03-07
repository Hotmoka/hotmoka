package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.hotmoka.beans.Marshallable;

/**
 * The response of a transaction.
 */
public abstract class TransactionResponse extends Marshallable {

	/**
	 * Used to marshal requests that are specific to a node.
	 * After this selector, the qualified name of the request must be follow.
	 */
	protected final static byte EXPANSION_SELECTOR = 15;

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the response could not be unmarshalled
	 * @throws ClassNotFoundException if the response could not be unmarshalled
	 */
	public static TransactionResponse from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte selector = ois.readByte();

		switch (selector) {
		case GameteCreationTransactionResponse.SELECTOR: return GameteCreationTransactionResponse.from(ois);
		case JarStoreInitialTransactionResponse.SELECTOR: return JarStoreInitialTransactionResponse.from(ois);
		case InitializationTransactionResponse.SELECTOR: return InitializationTransactionResponse.from(ois);
		case JarStoreTransactionFailedResponse.SELECTOR: return JarStoreTransactionFailedResponse.from(ois);
		case JarStoreTransactionSuccessfulResponse.SELECTOR: return JarStoreTransactionSuccessfulResponse.from(ois);
		case ConstructorCallTransactionExceptionResponse.SELECTOR: return ConstructorCallTransactionExceptionResponse.from(ois);
		case ConstructorCallTransactionFailedResponse.SELECTOR: return ConstructorCallTransactionFailedResponse.from(ois);
		case ConstructorCallTransactionSuccessfulResponse.SELECTOR:
		case ConstructorCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS: return ConstructorCallTransactionSuccessfulResponse.from(ois, selector);
		case MethodCallTransactionExceptionResponse.SELECTOR: return MethodCallTransactionExceptionResponse.from(ois);
		case MethodCallTransactionFailedResponse.SELECTOR: return MethodCallTransactionFailedResponse.from(ois);
		case MethodCallTransactionSuccessfulResponse.SELECTOR:
		case MethodCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS_NO_SELF_CHARGED:
		case MethodCallTransactionSuccessfulResponse.SELECTOR_ONE_EVENT_NO_SELF_CHARGED: return MethodCallTransactionSuccessfulResponse.from(ois, selector);
		case VoidMethodCallTransactionSuccessfulResponse.SELECTOR:
		case VoidMethodCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS_NO_SELF_CHARGED: return VoidMethodCallTransactionSuccessfulResponse.from(ois, selector);
		case EXPANSION_SELECTOR: {
			// this case deals with responses that only exist in a specific type of node;
			// hence their fully-qualified name must be available after the expansion selector

			String className = ois.readUTF();
			Class<?> clazz = Class.forName(className, false, ClassLoader.getSystemClassLoader());

			// only subclass of TransactionResponse are considered, to block potential call injections
			if (!TransactionResponse.class.isAssignableFrom(clazz))
				throw new IOException("unkown response class " + className);

			Method from;
			try {
				from = clazz.getMethod("from", ObjectInputStream.class);
			}
			catch (NoSuchMethodException | SecurityException e) {
				throw new IOException("cannot find method " + className + ".from(ObjectInputStream)");
			}

			try {
				return (TransactionResponse) from.invoke(null, ois);
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IOException("cannot call method " + className + ".from(ObjectInputStream)");
			}
		}
		default: throw new IOException("unexpected response selector: " + selector);
		}
	}

	protected static byte[] instrumentedJarFrom(ObjectInputStream ois) throws IOException {
		int instrumentedJarLength = ois.readInt();
		byte[] instrumentedJar = new byte[instrumentedJarLength];
		int howMany = ois.readNBytes(instrumentedJar, 0, instrumentedJarLength);
		if (instrumentedJarLength != howMany)
			throw new IOException("jar length mismatch: expected " + instrumentedJarLength + " but found " + howMany);

		return instrumentedJar;
	}
}