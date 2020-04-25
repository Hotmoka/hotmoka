package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.values.StorageReference;

/**
 * A request for a transaction that installs a jar in an initialized blockchain.
 */
@Immutable
public class JarStoreTransactionRequest extends NonInitialTransactionRequest<JarStoreTransactionResponse> implements AbstractJarStoreTransactionRequest {

	private static final long serialVersionUID = -986118537465436635L;
	final static byte SELECTOR = 3;

	/**
	 * The bytes of the jar to install.
	 */
	private final byte[] jar;

	/**
	 * The dependencies of the jar, already installed in blockchain
	 */
	private final Classpath[] dependencies;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 */
	public JarStoreTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, byte[] jar, Classpath... dependencies) {
		super(caller, nonce, gasLimit, gasPrice, classpath);

		this.jar = jar.clone();
		this.dependencies = dependencies;
	}

	@Override
	public byte[] getJar() {
		return jar.clone();
	}

	@Override
	public int getJarLength() {
		return jar.length;
	}

	@Override
	public Stream<Classpath> getDependencies() {
		return Stream.of(dependencies);
	}

	/**
	 * Yields the number of dependencies.
	 * 
	 * @return the number of dependencies
	 */
	public int getNumberOfDependencies() {
		return dependencies.length;
	}

	/**
	 * Yields the size of the jar to install (in bytes).
	 * 
	 * @return the size of the jar to install
	 */
	public final int getJarSize() {
		return jar.length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        for (byte b: jar)
            sb.append(String.format("%02x", b));

        return super.toString() + "\n"
			+ "  dependencies: " + Arrays.toString(dependencies) + "\n"
			+ "  jar: " + sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof JarStoreTransactionRequest) {
			JarStoreTransactionRequest otherCast = (JarStoreTransactionRequest) other;
			return super.equals(otherCast) && Arrays.equals(jar, otherCast.jar) && Arrays.equals(dependencies, otherCast.dependencies);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(jar) ^ Arrays.deepHashCode(dependencies);
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
		oos.writeInt(jar.length);
		oos.write(jar);
		oos.writeInt(dependencies.length);
		for (Classpath dependency: dependencies)
			dependency.into(oos);
	}
}