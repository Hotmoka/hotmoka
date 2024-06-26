/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.internal.responses;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.node.api.updates.Update;

/**
 * Implementation of a response for a failed transaction that should have called a constructor
 * of a storage class in blockchain.
 */
@Immutable
public class ConstructorCallTransactionFailedResponseImpl extends CodeExecutionTransactionResponseImpl implements ConstructorCallTransactionFailedResponse {
	final static byte SELECTOR = 5;

	/**
	 * The amount of gas consumed by the transaction as penalty for the failure.
	 */
	private final BigInteger gasConsumedForPenalty;

	/**
	 * The fully-qualified class name of the cause exception.
	 */
	private final String classNameOfCause;

	/**
	 * The message of the cause exception.
	 */
	private final String messageOfCause;

	/**
	 * The program point where the cause exception occurred.
	 */
	private final String where;

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
	public ConstructorCallTransactionFailedResponseImpl(String classNameOfCause, String messageOfCause, String where, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, BigInteger gasConsumedForPenalty) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.gasConsumedForPenalty = Objects.requireNonNull(gasConsumedForPenalty, "gasConsumedForPenalty cannot be null");
		this.classNameOfCause = Objects.requireNonNull(classNameOfCause, "classNameOfCause cannot be null");
		this.messageOfCause = messageOfCause == null ? "" : messageOfCause;
		this.where = where == null ? "" : where;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ConstructorCallTransactionFailedResponse cctfr && super.equals(other)
			&& gasConsumedForPenalty.equals(cctfr.getGasConsumedForPenalty())
			&& classNameOfCause.equals(cctfr.getClassNameOfCause())
			&& messageOfCause.equals(cctfr.getMessageOfCause()) && where.equals(cctfr.getWhere());
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ gasConsumedForPenalty.hashCode() ^ classNameOfCause.hashCode() ^ messageOfCause.hashCode() ^ where.hashCode();
	}

	@Override
	protected String gasToString() {
		return super.gasToString() + "  gas consumed for penalty: " + gasConsumedForPenalty + "\n";
	}

	@Override
	public final BigInteger getGasConsumedForPenalty() {
		return gasConsumedForPenalty;
	}

	@Override
	public final String getClassNameOfCause() {
		return classNameOfCause;
	}

	@Override
	public final String getMessageOfCause() {
		return messageOfCause;
	}

	@Override
	public final String getWhere() {
		return where;
	}

	@Override
	public String toString() {
        return super.toString()
        	+ "\n  cause: " + classNameOfCause + ":" + messageOfCause;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		super.into(context);
		context.writeBigInteger(gasConsumedForPenalty);
		context.writeStringUnshared(classNameOfCause);
		context.writeStringUnshared(messageOfCause);
		context.writeStringUnshared(where);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @return the response
	 * @throws IOException if the response cannot be unmarshalled
	 */
	public static ConstructorCallTransactionFailedResponseImpl from(UnmarshallingContext context) throws IOException {
		Stream<Update> updates = Stream.of(context.readLengthAndArray(Updates::from, Update[]::new));
		var gasConsumedForCPU = context.readBigInteger();
		var gasConsumedForRAM = context.readBigInteger();
		var gasConsumedForStorage = context.readBigInteger();
		var gasConsumedForPenalty = context.readBigInteger();
		var classNameOfCause = context.readStringUnshared();
		var messageOfCause = context.readStringUnshared();
		var where = context.readStringUnshared();
		return new ConstructorCallTransactionFailedResponseImpl(classNameOfCause, messageOfCause, where, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
	}
}