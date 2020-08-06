package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.updates.Update;

/**
 * A response for a successful transaction that installs a jar in a blockchain.
 */
@Immutable
public class JarStoreTransactionSuccessfulResponse extends JarStoreTransactionResponse implements TransactionResponseWithInstrumentedJar {
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
	 * Builds the transaction response.
	 * 
	 * @param instrumentedJar the bytes of the jar to install, instrumented
	 * @param dependencies the dependencies of the jar, previously installed in blockchain
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public JarStoreTransactionSuccessfulResponse(byte[] instrumentedJar, Stream<TransactionReference> dependencies, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.instrumentedJar = instrumentedJar.clone();
		this.dependencies = dependencies.toArray(TransactionReference[]::new);
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
		if (other instanceof JarStoreTransactionSuccessfulResponse) {
			JarStoreTransactionSuccessfulResponse otherCast = (JarStoreTransactionSuccessfulResponse) other;
			return super.equals(other) && Arrays.equals(instrumentedJar, otherCast.instrumentedJar)
				&& Arrays.equals(dependencies, otherCast.dependencies);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(instrumentedJar) ^ Arrays.hashCode(dependencies);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        for (byte b: instrumentedJar)
            sb.append(String.format("%02x", b));

        return super.toString() + "\n  instrumented jar: " + sb.toString();
	}

	@Override
	public TransactionReference getOutcomeAt(TransactionReference transactionReference) {
		// the outcome is the reference to the transaction where this response has been executed
		return transactionReference;
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOfBytes(instrumentedJar.length))
			.add(getDependencies().map(dependency -> gasCostModel.storageCostOf(dependency)).reduce(BigInteger.ZERO, BigInteger::add));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeByte(SELECTOR);
		super.into(context);
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
	public static JarStoreTransactionSuccessfulResponse from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
		BigInteger gasConsumedForCPU = unmarshallBigInteger(ois);
		BigInteger gasConsumedForRAM = unmarshallBigInteger(ois);
		BigInteger gasConsumedForStorage = unmarshallBigInteger(ois);
		byte[] instrumentedJar = instrumentedJarFrom(ois);
		Stream<TransactionReference> dependencies = Stream.of(unmarshallingOfArray(TransactionReference::from, TransactionReference[]::new, ois));
		return new JarStoreTransactionSuccessfulResponse(instrumentedJar, dependencies, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}
}