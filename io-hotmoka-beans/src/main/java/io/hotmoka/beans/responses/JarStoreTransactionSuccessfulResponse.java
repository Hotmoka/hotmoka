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

package io.hotmoka.beans.responses;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.Updates;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * A response for a successful transaction that installs a jar in a blockchain.
 */
@Immutable
public class JarStoreTransactionSuccessfulResponse extends JarStoreNonInitialTransactionResponse implements TransactionResponseWithInstrumentedJar {
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
	public JarStoreTransactionSuccessfulResponse(byte[] instrumentedJar, Stream<TransactionReference> dependencies, long verificationToolVersion, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.instrumentedJar = instrumentedJar.clone();
		this.dependencies = dependencies.toArray(TransactionReference[]::new);
		Stream.of(this.dependencies).forEach(dependency -> Objects.requireNonNull(dependency, "dependencies cannot hold null"));
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
		return other instanceof JarStoreTransactionSuccessfulResponse jstsr && super.equals(other)
			&& Arrays.equals(instrumentedJar, jstsr.instrumentedJar) && Arrays.equals(dependencies, jstsr.dependencies);
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
		context.writeLong(verificationToolVersion);
		context.writeLengthAndBytes(instrumentedJar);
		context.writeLengthAndArray(dependencies);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @return the response
	 * @throws IOException if the response could not be unmarshalled
	 */
	public static JarStoreTransactionSuccessfulResponse from(UnmarshallingContext context) throws IOException {
		Stream<Update> updates = Stream.of(context.readLengthAndArray(Updates::from, Update[]::new));
		var gasConsumedForCPU = context.readBigInteger();
		var gasConsumedForRAM = context.readBigInteger();
		var gasConsumedForStorage = context.readBigInteger();
		var verificationToolVersion = context.readLong();
		byte[] instrumentedJar = context.readLengthAndBytes("Jar length mismatch in response");
		Stream<TransactionReference> dependencies = Stream.of(context.readLengthAndArray(TransactionReferences::from, TransactionReference[]::new));
		return new JarStoreTransactionSuccessfulResponse(instrumentedJar, dependencies, verificationToolVersion, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	@Override
	public long getVerificationVersion() {
		return verificationToolVersion;
	}
}