package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;

/**
 * A response for a successful transaction that calls a constructor of a storage
 * class in blockchain. The constructor is annotated as {@link io.takamaka.code.lang.ThrowsExceptions}.
 * It has been called without problems but it threw an instance of {@link java.lang.Exception}.
 */
@Immutable
public class ConstructorCallTransactionExceptionResponse extends ConstructorCallTransactionResponse implements TransactionResponseWithEvents {
	final static byte SELECTOR = 4;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * The fully-qualified class name of the cause exception.
	 */
	public final String classNameOfCause;

	/**
	 * The message of the cause exception.
	 */
	public final String messageOfCause;

	/**
	 * The program point where the cause exception occurred.
	 */
	public final String where;

	/**
	 * Builds the transaction response.
	 * 
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 * @param where the program point where the cause exception occurred; this might be {@code null}
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public ConstructorCallTransactionExceptionResponse(String classNameOfCause, String messageOfCause, String where, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.events = events.toArray(StorageReference[]::new);
		this.classNameOfCause = classNameOfCause;
		this.messageOfCause = messageOfCause == null ? "" : messageOfCause;
		this.where = where == null ? "" : where;
	}

	@Override
	public Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ConstructorCallTransactionExceptionResponse) {
			ConstructorCallTransactionExceptionResponse otherCast = (ConstructorCallTransactionExceptionResponse) other;
			return super.equals(other) && Arrays.equals(events, otherCast.events)
				&& classNameOfCause.equals(otherCast.classNameOfCause)
				&& messageOfCause.equals(otherCast.messageOfCause)
				&& where.equals(otherCast.where);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(events) ^ classNameOfCause.hashCode()
			^ messageOfCause.hashCode() ^ where.hashCode();
	}

	@Override
	public String toString() {
		if (messageOfCause.isEmpty())
			return super.toString() + "\n  throws: " + classNameOfCause + "\n  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
		else
			return super.toString() + "\n  throws: " + classNameOfCause + ":" + messageOfCause + "\n  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public StorageReference getOutcome() throws CodeExecutionException {
		throw new CodeExecutionException(classNameOfCause, messageOfCause, where);
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
		intoArrayWithoutSelector(events, oos);
		oos.writeUTF(classNameOfCause);
		oos.writeUTF(messageOfCause);
		oos.writeUTF(where);
	}
}