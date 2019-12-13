package io.takamaka.code.blockchain.request;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;

import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.GasCostModel;
import io.takamaka.code.blockchain.InitialTransactionRequest;
import io.takamaka.code.blockchain.UpdateOfBalance;
import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A request for a transaction that installs a jar in a yet not initialized blockchain.
 */
@Immutable
public class JarStoreInitialTransactionRequest implements InitialTransactionRequest, AbstractJarStoreTransactionRequest {

	private static final long serialVersionUID = -3166257105103213569L;

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
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 */
	public JarStoreInitialTransactionRequest(byte[] jar, Classpath... dependencies) {
		this.jar = jar.clone();
		this.dependencies = dependencies.clone();
	}

	@Override
	public byte[] getJar() {
		return jar.clone();
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
	public BigInteger size(GasCostModel gasCostModel) {
		// this request is for a free transaction, at initialization of the blockchain
		return BigInteger.ZERO;
	}

	@Override
	public boolean hasMinimalGas(UpdateOfBalance balanceUpdateInCaseOfFailure, GasCostModel gasCostModel) {
		// this request is for a free transaction, at initialization of the blockchain
		return true;
	}
}