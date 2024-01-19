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
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.responses.InitialTransactionResponse;
import io.hotmoka.beans.api.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.internal.responses.TransactionResponseImpl;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * A response for a transaction that installs a jar in a yet not initialized node.
 */
@Immutable
public class JarStoreInitialTransactionResponse extends TransactionResponseImpl implements InitialTransactionResponse, TransactionResponseWithInstrumentedJar, JarStoreTransactionResponse {
	public final static byte SELECTOR = 1;

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
	 */
	public JarStoreInitialTransactionResponse(byte[] instrumentedJar, Stream<TransactionReference> dependencies, long verificationToolVersion) {
		this.instrumentedJar = instrumentedJar.clone();
		this.dependencies = dependencies.toArray(TransactionReference[]::new);
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
		return other instanceof JarStoreInitialTransactionResponse jsitr &&
			Arrays.equals(instrumentedJar, jsitr.instrumentedJar) && Arrays.equals(dependencies, jsitr.dependencies);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(instrumentedJar) ^ Arrays.hashCode(dependencies);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
        for (byte b: instrumentedJar)
            sb.append(String.format("%02x", b));

        return getClass().getSimpleName() + ":\n  verified with verification version " + verificationToolVersion + "\n  instrumented jar: " + sb;
	}

	/**
	 * Yields the outcome of the execution having this response, performed
	 * at the given transaction reference.
	 * 
	 * @param transactionReference the transaction reference
	 * @return the outcome
	 */
	public TransactionReference getOutcomeAt(TransactionReference transactionReference) {
		// the result of installing a jar in a node is the reference to the transaction that installed the jar
		return transactionReference;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
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
	public static JarStoreInitialTransactionResponse from(UnmarshallingContext context) throws IOException {
		var verificationToolVersion = context.readLong();
		byte[] instrumentedJar = context.readLengthAndBytes("Jar length mismatch in response");
		Stream<TransactionReference> dependencies = Stream.of(context.readLengthAndArray(TransactionReferences::from, TransactionReference[]::new));
		return new JarStoreInitialTransactionResponse(instrumentedJar, dependencies, verificationToolVersion);
	}

	@Override
	public long getVerificationVersion() {
		return verificationToolVersion;
	}
}