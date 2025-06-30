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
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.internal.json.TransactionResponseJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of a response for a failed transaction that should have installed a jar in the node.
 */
@Immutable
public class JarStoreTransactionFailedResponseImpl extends NonInitialTransactionResponseImpl implements JarStoreTransactionFailedResponse {
	final static byte SELECTOR = 3;

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
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param gasConsumedForPenalty the amount of gas consumed by the transaction as penalty for the failure
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 */
	public JarStoreTransactionFailedResponseImpl(Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, BigInteger gasConsumedForPenalty, String classNameOfCause, String messageOfCause) {
		this(updates.toArray(Update[]::new), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty, classNameOfCause, messageOfCause, IllegalArgumentException::new);
	}

	/**
	 * Unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the response could not be unmarshalled
	 */
	public JarStoreTransactionFailedResponseImpl(UnmarshallingContext context) throws IOException {
		this(
			context.readLengthAndArray(Updates::from, Update[]::new),
			context.readBigInteger(),
			context.readBigInteger(),
			context.readBigInteger(),
			context.readBigInteger(),
			context.readStringUnshared(),
			context.readStringUnshared(),
			IOException::new
		);
	}

	/**
	 * Creates a response from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public JarStoreTransactionFailedResponseImpl(TransactionResponseJson json) throws InconsistentJsonException {
		this(
			unmapUpdates(json),
			json.getGasConsumedForCPU(),
			json.getGasConsumedForRAM(),
			json.getGasConsumedForStorage(),
			json.getGasConsumedForPenalty(),
			json.getClassNameOfCause(),
			json.getMessageOfCause(),
			InconsistentJsonException::new
		);
	}

	/**
	 * Builds the transaction response.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param gasConsumedForPenalty the amount of gas consumed by the transaction as penalty for the failure
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> JarStoreTransactionFailedResponseImpl(Update[] updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, BigInteger gasConsumedForPenalty, String classNameOfCause, String messageOfCause, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, onIllegalArgs);

		this.gasConsumedForPenalty = Objects.requireNonNull(gasConsumedForPenalty, "gasConsumedForPenalty cannot be null", onIllegalArgs);
		this.classNameOfCause = Objects.requireNonNull(classNameOfCause, "classNameOfCause cannot be null", onIllegalArgs);
		this.messageOfCause = messageOfCause == null ? "" : messageOfCause;
	}

	@Override
	protected String gasToString() {
		return super.gasToString() + "  gas consumed for penalty: " + gasConsumedForPenalty + "\n";
	}

	@Override
	public BigInteger getGasConsumedForPenalty() {
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
		return other instanceof JarStoreTransactionFailedResponse jstfr && super.equals(other)
			&& gasConsumedForPenalty.equals(jstfr.getGasConsumedForPenalty())
			&& classNameOfCause.equals(jstfr.getClassNameOfCause()) && messageOfCause.equals(jstfr.getMessageOfCause());
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
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		super.into(context);
		context.writeBigInteger(gasConsumedForPenalty);
		context.writeStringUnshared(classNameOfCause);
		context.writeStringUnshared(messageOfCause);
	}
}