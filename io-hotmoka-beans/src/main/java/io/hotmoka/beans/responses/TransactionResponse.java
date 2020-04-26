package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.internal.UnmarshallingUtils;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * The response of a transaction.
 */
public interface TransactionResponse extends Serializable {

	boolean equals(Object other);

	int hashCode();

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
		case GameteCreationTransactionResponse.SELECTOR: {
			return new GameteCreationTransactionResponse(updatesFrom(ois), (StorageReference) StorageValue.from(ois));
		}
		case JarStoreInitialTransactionResponse.SELECTOR: {
			return new JarStoreInitialTransactionResponse(instrumentedJarFrom(ois), dependenciesFrom(ois));
		}
		case JarStoreTransactionFailedResponse.SELECTOR: {
			Stream<Update> updates = updatesFrom(ois);
			BigInteger gasConsumedForCPU = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForPenalty = UnmarshallingUtils.unmarshallBigInteger(ois);
			String classNameOfCause = ois.readUTF();
			String messageOfCause = ois.readUTF();

			return new JarStoreTransactionFailedResponse(classNameOfCause, messageOfCause, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		}
		case JarStoreTransactionSuccessfulResponse.SELECTOR: {
			Stream<Update> updates = updatesFrom(ois);
			BigInteger gasConsumedForCPU = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = UnmarshallingUtils.unmarshallBigInteger(ois);
			byte[] instrumentedJar = instrumentedJarFrom(ois);
			Stream<Classpath> dependencies = dependenciesFrom(ois);

			return new JarStoreTransactionSuccessfulResponse(instrumentedJar, dependencies, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case ConstructorCallTransactionExceptionResponse.SELECTOR: {
			Stream<Update> updates = updatesFrom(ois);
			BigInteger gasConsumedForCPU = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = UnmarshallingUtils.unmarshallBigInteger(ois);
			Stream<StorageReference> events = eventsFrom(ois);
			String classNameOfCause = ois.readUTF();
			String messageOfCause = ois.readUTF();
			String where = ois.readUTF();

			return new ConstructorCallTransactionExceptionResponse(classNameOfCause, messageOfCause, where, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case ConstructorCallTransactionFailedResponse.SELECTOR: {
			Stream<Update> updates = updatesFrom(ois);
			BigInteger gasConsumedForCPU = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForPenalty = UnmarshallingUtils.unmarshallBigInteger(ois);
			String classNameOfCause = ois.readUTF();
			String messageOfCause = ois.readUTF();
			String where = ois.readUTF();

			return new ConstructorCallTransactionFailedResponse(classNameOfCause, messageOfCause, where, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		}
		case ConstructorCallTransactionSuccessfulResponse.SELECTOR: {
			Stream<Update> updates = updatesFrom(ois);
			BigInteger gasConsumedForCPU = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = UnmarshallingUtils.unmarshallBigInteger(ois);
			Stream<StorageReference> events = eventsFrom(ois);
			StorageReference newObject = (StorageReference) StorageValue.from(ois);

			return new ConstructorCallTransactionSuccessfulResponse(newObject, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case MethodCallTransactionExceptionResponse.SELECTOR: {
			Stream<Update> updates = updatesFrom(ois);
			BigInteger gasConsumedForCPU = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = UnmarshallingUtils.unmarshallBigInteger(ois);
			Stream<StorageReference> events = eventsFrom(ois);
			String classNameOfCause = ois.readUTF();
			String messageOfCause = ois.readUTF();
			String where = ois.readUTF();

			return new MethodCallTransactionExceptionResponse(classNameOfCause, messageOfCause, where, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case MethodCallTransactionFailedResponse.SELECTOR: {
			Stream<Update> updates = updatesFrom(ois);
			BigInteger gasConsumedForCPU = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForPenalty = UnmarshallingUtils.unmarshallBigInteger(ois);
			String classNameOfCause = ois.readUTF();
			String messageOfCause = ois.readUTF();
			String where = ois.readUTF();

			return new MethodCallTransactionFailedResponse(classNameOfCause, messageOfCause, where, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		}
		case MethodCallTransactionSuccessfulResponse.SELECTOR: {
			Stream<Update> updates = updatesFrom(ois);
			BigInteger gasConsumedForCPU = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = UnmarshallingUtils.unmarshallBigInteger(ois);
			StorageValue result = StorageValue.from(ois);
			Stream<StorageReference> events = eventsFrom(ois);

			return new MethodCallTransactionSuccessfulResponse(result, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		case VoidMethodCallTransactionSuccessfulResponse.SELECTOR: {
			Stream<Update> updates = updatesFrom(ois);
			BigInteger gasConsumedForCPU = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForRAM = UnmarshallingUtils.unmarshallBigInteger(ois);
			BigInteger gasConsumedForStorage = UnmarshallingUtils.unmarshallBigInteger(ois);
			Stream<StorageReference> events = eventsFrom(ois);			

			return new VoidMethodCallTransactionSuccessfulResponse(updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		default: throw new IOException("unexpected response selector: " + selector);
		}
	}

	private static Stream<StorageReference> eventsFrom(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		int eventsLength = ois.readInt();
		StorageReference[] events = new StorageReference[eventsLength];
		for (int pos = 0; pos < eventsLength; pos++)
			events[pos] = (StorageReference) StorageValue.from(ois);

		return Stream.of(events);
	}

	private static Stream<Classpath> dependenciesFrom(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		int dependenciesLength = ois.readInt();
		Classpath[] dependencies = new Classpath[dependenciesLength];
		for (int pos = 0; pos < dependenciesLength; pos++)
			dependencies[pos] = Classpath.from(ois);

		return Stream.of(dependencies);
	}

	private static byte[] instrumentedJarFrom(ObjectInputStream ois) throws IOException {
		int instrumentedJarLength = ois.readInt();
		byte[] instrumentedJar = new byte[instrumentedJarLength];
		if (instrumentedJarLength != ois.read(instrumentedJar))
			throw new IOException("jar length mismatch");

		return instrumentedJar;
	}

	private static Stream<Update> updatesFrom(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		int updatesLength = ois.readInt();
		Update[] updates = new Update[updatesLength];
		for (int pos = 0; pos < updatesLength; pos++)
			updates[pos] = Update.from(ois);

		return Stream.of(updates);
	}
}