package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * The response of a transaction.
 */
public abstract class TransactionResponse extends Marshallable {

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
		case GameteCreationTransactionResponse.SELECTOR: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			return new GameteCreationTransactionResponse(updates, StorageReference.from(ois));
		}
		case JarStoreInitialTransactionResponse.SELECTOR: {
			Stream<Classpath> dependencies = Stream.of(unmarshallingOfArray(Classpath::from, Classpath[]::new, ois));
			return new JarStoreInitialTransactionResponse(instrumentedJarFrom(ois), dependencies);
		}
		case JarStoreTransactionFailedResponse.SELECTOR: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			BigInteger gasConsumedForPenalty = unmarshallBigInteger(ois);
			String classNameOfCause = ois.readUTF();
			String messageOfCause = ois.readUTF();

			return new JarStoreTransactionFailedResponse(classNameOfCause, messageOfCause, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		}
		case JarStoreTransactionSuccessfulResponse.SELECTOR: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			byte[] instrumentedJar = instrumentedJarFrom(ois);
			Stream<Classpath> dependencies = Stream.of(unmarshallingOfArray(Classpath::from, Classpath[]::new, ois));

			return new JarStoreTransactionSuccessfulResponse(instrumentedJar, dependencies, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case ConstructorCallTransactionExceptionResponse.SELECTOR: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			Stream<StorageReference> events = Stream.of(unmarshallingOfArray(StorageReference::from, StorageReference[]::new, ois));
			String classNameOfCause = ois.readUTF();
			String messageOfCause = ois.readUTF();
			String where = ois.readUTF();

			return new ConstructorCallTransactionExceptionResponse(classNameOfCause, messageOfCause, where, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case ConstructorCallTransactionFailedResponse.SELECTOR: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			BigInteger gasConsumedForPenalty = unmarshallBigInteger(ois);
			String classNameOfCause = ois.readUTF();
			String messageOfCause = ois.readUTF();
			String where = ois.readUTF();

			return new ConstructorCallTransactionFailedResponse(classNameOfCause, messageOfCause, where, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		}
		case ConstructorCallTransactionSuccessfulResponse.SELECTOR: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			Stream<StorageReference> events = Stream.of(unmarshallingOfArray(StorageReference::from, StorageReference[]::new, ois));
			StorageReference newObject = StorageReference.from(ois);

			return new ConstructorCallTransactionSuccessfulResponse(newObject, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case ConstructorCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			Stream<StorageReference> events = Stream.empty();
			StorageReference newObject = StorageReference.from(ois);

			return new ConstructorCallTransactionSuccessfulResponse(newObject, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case MethodCallTransactionExceptionResponse.SELECTOR: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			Stream<StorageReference> events = Stream.of(unmarshallingOfArray(StorageReference::from, StorageReference[]::new, ois));
			String classNameOfCause = ois.readUTF();
			String messageOfCause = ois.readUTF();
			String where = ois.readUTF();

			return new MethodCallTransactionExceptionResponse(classNameOfCause, messageOfCause, where, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case MethodCallTransactionFailedResponse.SELECTOR: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			BigInteger gasConsumedForPenalty = unmarshallBigInteger(ois);
			String classNameOfCause = ois.readUTF();
			String messageOfCause = ois.readUTF();
			String where = ois.readUTF();

			return new MethodCallTransactionFailedResponse(classNameOfCause, messageOfCause, where, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		}
		case MethodCallTransactionSuccessfulResponse.SELECTOR: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			StorageValue result = StorageValue.from(ois);
			Stream<StorageReference> events = Stream.of(unmarshallingOfArray(StorageReference::from, StorageReference[]::new, ois));

			return new MethodCallTransactionSuccessfulResponse(result, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case MethodCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			StorageValue result = StorageValue.from(ois);
			Stream<StorageReference> events = Stream.empty();

			return new MethodCallTransactionSuccessfulResponse(result, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case VoidMethodCallTransactionSuccessfulResponse.SELECTOR: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			Stream<StorageReference> events = Stream.of(unmarshallingOfArray(StorageReference::from, StorageReference[]::new, ois));		

			return new VoidMethodCallTransactionSuccessfulResponse(updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case VoidMethodCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS: {
			Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
			BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
			Stream<StorageReference> events = Stream.empty();		

			return new VoidMethodCallTransactionSuccessfulResponse(updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		default: throw new IOException("unexpected response selector: " + selector);
		}
	}

	private static byte[] instrumentedJarFrom(ObjectInputStream ois) throws IOException {
		int instrumentedJarLength = ois.readInt();
		byte[] instrumentedJar = new byte[instrumentedJarLength];
		if (instrumentedJarLength != ois.readNBytes(instrumentedJar, 0, instrumentedJarLength))
			throw new IOException("jar length mismatch");

		return instrumentedJar;
	}
}