package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;

/**
 * A response for a transaction that installs a jar in a yet not initialized node.
 */
@Immutable
public class JarStoreInitialTransactionResponse extends InitialTransactionResponse implements TransactionResponseWithInstrumentedJar, JarStoreTransactionResponse {
	final static byte SELECTOR = 1;

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
	private final int verificationToolVersion;
	
	/**
	 * Builds the transaction response.
	 * 
	 * @param instrumentedJar the bytes of the jar to install, instrumented
	 * @param dependencies the dependencies of the jar, previously installed in blockchain
	 */
	public JarStoreInitialTransactionResponse(byte[] instrumentedJar, Stream<TransactionReference> dependencies, int verificationToolVersion) {
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
		if (other instanceof JarStoreInitialTransactionResponse) {
			JarStoreInitialTransactionResponse otherCast = (JarStoreInitialTransactionResponse) other;
			return Arrays.equals(instrumentedJar, otherCast.instrumentedJar) && Arrays.equals(dependencies, otherCast.dependencies);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(instrumentedJar) ^ Arrays.hashCode(dependencies);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        for (byte b: instrumentedJar)
            sb.append(String.format("%02x", b));

        return getClass().getSimpleName() + ":\n  instrumented jar: " + sb.toString();
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
		context.oos.writeByte(SELECTOR);
		context.oos.writeInt(verificationToolVersion);
		context.oos.writeInt(instrumentedJar.length);
		context.oos.write(instrumentedJar);
		intoArray(dependencies, context);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the response could not be unmarshalled
	 * @throws ClassNotFoundException if the response could not be unmarshalled
	 */
	public static JarStoreInitialTransactionResponse from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		int verificationToolVersion = ois.readInt();
		byte[] instrumentedJar = instrumentedJarFrom(ois);
		Stream<TransactionReference> dependencies = Stream.of(unmarshallingOfArray(TransactionReference::from, TransactionReference[]::new, ois));
		return new JarStoreInitialTransactionResponse(instrumentedJar, dependencies, verificationToolVersion);
	}

	@Override
	public int getVerificationToolVersion() {
	
		return verificationToolVersion;
	}
}