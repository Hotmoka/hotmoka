package io.hotmoka.beans.responses;

import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;

/**
 * A response for a failed transaction that should have called a constructor
 * of a storage class in blockchain.
 */
@Immutable
public class ConstructorCallTransactionFailedResponse extends ConstructorCallTransactionResponse implements TransactionResponseFailed {
	final static byte SELECTOR = 5;

	/**
	 * The amount of gas consumed by the transaction as penalty for the failure.
	 */
	public final BigInteger gasConsumedForPenalty;

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
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param gasConsumedForPenalty the amount of gas consumed by the transaction as penalty for the failure
	 */
	public ConstructorCallTransactionFailedResponse(String classNameOfCause, String messageOfCause, String where, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, BigInteger gasConsumedForPenalty) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.gasConsumedForPenalty = gasConsumedForPenalty;
		this.classNameOfCause = classNameOfCause;
		this.messageOfCause = messageOfCause == null ? "" : messageOfCause;
		this.where = where == null ? "" : where;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ConstructorCallTransactionFailedResponse) {
			ConstructorCallTransactionFailedResponse otherCast = (ConstructorCallTransactionFailedResponse) other;
			return super.equals(other) && gasConsumedForPenalty.equals(otherCast.gasConsumedForPenalty)
				&& classNameOfCause.equals(otherCast.classNameOfCause)
				&& messageOfCause.equals(otherCast.messageOfCause)
				&& where.equals(otherCast.where);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ gasConsumedForPenalty.hashCode() ^ classNameOfCause.hashCode()
			^ messageOfCause.hashCode() ^ where.hashCode();
	}

	@Override
	protected String gasToString() {
		return super.gasToString() + "  gas consumed for penalty: " + gasConsumedForPenalty + "\n";
	}

	@Override
	public BigInteger gasConsumedForPenalty() {
		return gasConsumedForPenalty;
	}

	@Override
	public String getClassNameOfCause() {
		return classNameOfCause;
	}

	@Override
	public String getMessageOfCause() {
		return messageOfCause;
	}

	@Override
	public String toString() {
        return super.toString()
        	+ "\n  cause: " + classNameOfCause + ":" + messageOfCause;
	}

	@Override
	public StorageReference getOutcome() throws TransactionException {
		throw new TransactionException(classNameOfCause, messageOfCause, where);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel)
			.add(gasCostModel.storageCostOf(gasConsumedForPenalty))
			.add(gasCostModel.storageCostOf(classNameOfCause))
			.add(gasCostModel.storageCostOf(messageOfCause))
			.add(gasCostModel.storageCostOf(where));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeByte(SELECTOR);
		super.into(context);
		marshal(gasConsumedForPenalty, context);
		context.oos.writeUTF(classNameOfCause);
		context.oos.writeUTF(messageOfCause);
		context.oos.writeUTF(where);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the response could not be unmarshalled
	 * @throws ClassNotFoundException if the response could not be unmarshalled
	 */
	public static ConstructorCallTransactionFailedResponse from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, context));
		BigInteger gasConsumedForCPU = unmarshallBigInteger(context);
		BigInteger gasConsumedForRAM = unmarshallBigInteger(context);
		BigInteger gasConsumedForStorage = unmarshallBigInteger(context);
		BigInteger gasConsumedForPenalty = unmarshallBigInteger(context);
		String classNameOfCause = context.ois.readUTF();
		String messageOfCause = context.ois.readUTF();
		String where = context.ois.readUTF();
		return new ConstructorCallTransactionFailedResponse(classNameOfCause, messageOfCause, where, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
	}
}