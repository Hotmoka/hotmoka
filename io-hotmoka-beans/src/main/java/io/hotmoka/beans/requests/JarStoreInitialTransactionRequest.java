package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;

/**
 * A request for a transaction that installs a jar in a yet not initialized node.
 */
@Immutable
public class JarStoreInitialTransactionRequest extends InitialTransactionRequest<JarStoreInitialTransactionResponse> implements AbstractJarStoreTransactionRequest {
	final static byte SELECTOR = 1;

	/**
	 * The bytes of the jar to install.
	 */
	private final byte[] jar;

	/**
	 * The dependencies of the jar, already installed in blockchain
	 */
	private final TransactionReference[] dependencies;

	/**
	 * Builds the transaction request.
	 * 
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 */
	public JarStoreInitialTransactionRequest(byte[] jar, TransactionReference... dependencies) {
		this.jar = jar.clone();
		this.dependencies = dependencies.clone();
	}

	@Override
	public final byte[] getJar() {
		return jar.clone();
	}

	@Override
	public final int getJarLength() {
		return jar.length;
	}

	@Override
	public final Stream<TransactionReference> getDependencies() {
		return Stream.of(dependencies);
	}

	/**
	 * Yields the number of dependencies.
	 * 
	 * @return the number of dependencies
	 */
	public final int getNumberOfDependencies() {
		return dependencies.length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        for (byte b: jar)
            sb.append(String.format("%02x", b));

        return getClass().getSimpleName() + ":\n"
			+ "  dependencies: " + Arrays.toString(dependencies) + "\n"
			+ "  jar: " + sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof JarStoreInitialTransactionRequest) {
			JarStoreInitialTransactionRequest otherCast = (JarStoreInitialTransactionRequest) other;
			return Arrays.equals(dependencies, otherCast.dependencies) && Arrays.equals(jar, otherCast.jar);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(jar) ^ Arrays.deepHashCode(dependencies);
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		oos.writeInt(jar.length);
		oos.write(jar);
		intoArray(dependencies, oos);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static JarStoreInitialTransactionRequest from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		int jarLength = ois.readInt();
		byte[] jar = new byte[jarLength];
		if (jarLength != ois.readNBytes(jar, 0, jarLength))
			throw new IOException("jar length mismatch in request");

		TransactionReference[] dependencies = unmarshallingOfArray(TransactionReference::from, TransactionReference[]::new, ois);

		return new JarStoreInitialTransactionRequest(jar, dependencies);
	}
}