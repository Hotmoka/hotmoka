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
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.internal.json.TransactionResponseJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of a response for a successful transaction that installs a jar in a blockchain.
 */
@Immutable
public class JarStoreTransactionSuccessfulResponseImpl extends NonInitialTransactionResponseImpl implements JarStoreTransactionSuccessfulResponse {
	final static byte SELECTOR = 2;

	/**
	 * The bytes of the jar to install, instrumented.
	 */
	private final byte[] instrumentedJar;

	/**
	 * The dependencies of the jar, previously installed in blockchain.
	 * This is a copy of the same information contained in the request.
	 */
	private final TransactionReference[] dependencies;
	
	/**
	 * the version of the verification tool involved in the verification process
	 */
	private final long verificationToolVersion;

	/**
	 * Builds the transaction response.
	 * 
	 * @param instrumentedJar the bytes of the jar to install, instrumented
	 * @param dependencies the dependencies of the jar, previously installed in blockchain
	 * @param verificationToolVersion the version of the verification tool
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public JarStoreTransactionSuccessfulResponseImpl(byte[] instrumentedJar, Stream<TransactionReference> dependencies, long verificationToolVersion, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		this(updates.toArray(Update[]::new), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, instrumentedJar.clone(), dependencies.toArray(TransactionReference[]::new), verificationToolVersion, IllegalArgumentException::new);
	}

	/**
	 * Unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the response could not be unmarshalled
	 */
	public JarStoreTransactionSuccessfulResponseImpl(UnmarshallingContext context) throws IOException {
		this(
			context.readLengthAndArray(Updates::from, Update[]::new),
			context.readBigInteger(),
			context.readBigInteger(),
			context.readBigInteger(),
			context.readLengthAndBytes("Jar length mismatch in response"),
			context.readLengthAndArray(TransactionReferences::from, TransactionReference[]::new),
			context.readLong(),
			IOException::new
		);
	}

	/**
	 * Creates a response from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public JarStoreTransactionSuccessfulResponseImpl(TransactionResponseJson json) throws InconsistentJsonException {
		this(
			unmapUpdates(json),
			json.getGasConsumedForCPU(),
			json.getGasConsumedForRAM(),
			json.getGasConsumedForStorage(),
			instrumentedJarAsBytes(json),
			unmapDependencies(json),
			json.getVerificationToolVersion(),
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
	 * @param instrumentedJar the bytes of the jar to install, instrumented
	 * @param dependencies the dependencies of the jar, previously installed in blockchain
	 * @param verificationToolVersion the version of the verification tool
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> JarStoreTransactionSuccessfulResponseImpl(Update[] updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, byte[] instrumentedJar, TransactionReference[] dependencies, long verificationToolVersion, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, onIllegalArgs);

		this.instrumentedJar = Objects.requireNonNull(instrumentedJar, "instrumenteJar cannot be null", onIllegalArgs);

		this.dependencies = Objects.requireNonNull(dependencies, "dependencies cannot be null", onIllegalArgs);
		for (var dependency: dependencies)
			Objects.requireNonNull(dependency, "dependencies cannot hold null elements", onIllegalArgs);

		this.verificationToolVersion = verificationToolVersion;
	}

	@Override
	public byte[] getInstrumentedJar() {
		return instrumentedJar.clone();
	}

	@Override
	public int getInstrumentedJarLength() {
		return instrumentedJar.length;
	}

	@Override
	public Stream<TransactionReference> getDependencies() {
		return Stream.of(dependencies);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof JarStoreTransactionSuccessfulResponseImpl jstsri) // optimization
			return super.equals(other)
				&& Arrays.equals(instrumentedJar, jstsri.instrumentedJar)
				&& Arrays.equals(dependencies, jstsri.dependencies);
		else
			return other instanceof JarStoreTransactionSuccessfulResponse jstsr && super.equals(other)
				&& Arrays.equals(instrumentedJar, jstsr.getInstrumentedJar())
				&& Arrays.equals(dependencies, jstsr.getDependencies().toArray(TransactionReference[]::new));
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(instrumentedJar) ^ Arrays.hashCode(dependencies);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
        for (byte b: instrumentedJar)
            sb.append(String.format("%02x", b));

        return super.toString() + "\n  verified with verification version " + verificationToolVersion + "\n  instrumented jar: " + sb;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		super.into(context);
		context.writeLengthAndBytes(instrumentedJar);
		context.writeLengthAndArray(dependencies);
		context.writeLong(verificationToolVersion);
	}

	@Override
	public long getVerificationVersion() {
		return verificationToolVersion;
	}
}