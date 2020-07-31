package io.hotmoka.takamaka.beans.responses;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.TransactionResponseFailed;
import io.hotmoka.beans.updates.Update;

/**
 * A response for a failed transaction that should have added or reduced the coins of an account.
 */
@Immutable
public class MintTransactionFailedResponse extends MintTransactionResponse implements TransactionResponseFailed {
	final static byte SELECTOR = 3; // TODO

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
	 * Builds the transaction response.
	 * 
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param gasConsumedForPenalty the amount of gas consumed by the transaction as penalty for the failure
	 */
	public MintTransactionFailedResponse(String classNameOfCause, String messageOfCause, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, BigInteger gasConsumedForPenalty) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.classNameOfCause = classNameOfCause;
		this.messageOfCause = messageOfCause == null ? "" : messageOfCause;
		this.gasConsumedForPenalty = gasConsumedForPenalty;
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
	public boolean equals(Object other) {
		if (other instanceof MintTransactionFailedResponse) {
			MintTransactionFailedResponse otherCast = (MintTransactionFailedResponse) other;
			return super.equals(other) && gasConsumedForPenalty.equals(otherCast.gasConsumedForPenalty)
				&& classNameOfCause.equals(classNameOfCause) && messageOfCause.equals(otherCast.messageOfCause);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ gasConsumedForPenalty.hashCode() ^ classNameOfCause.hashCode() ^ messageOfCause.hashCode();
	}

	@Override
	public String toString() {
        return super.toString()
        	+ "\n  cause: " + classNameOfCause + ":" + messageOfCause;
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel)
			.add(gasCostModel.storageCostOf(gasConsumedForPenalty))
			.add(gasCostModel.storageCostOf(classNameOfCause))
			.add(gasCostModel.storageCostOf(messageOfCause));
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
		marshal(gasConsumedForPenalty, oos);
		oos.writeUTF(classNameOfCause);
		oos.writeUTF(messageOfCause);
	}
}