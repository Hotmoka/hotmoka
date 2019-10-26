package takamaka.blockchain.request;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;

import io.takamaka.code.annotations.Immutable;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.GasCosts;
import takamaka.blockchain.UpdateOfBalance;
import takamaka.blockchain.response.JarStoreTransactionFailedResponse;
import takamaka.blockchain.values.StorageReference;

/**
 * A request for a transaction that installs a jar in an initialized blockchain.
 */
@Immutable
public class JarStoreTransactionRequest implements TransactionRequest, AbstractJarStoreTransactionRequest {

	private static final long serialVersionUID = -986118537465436635L;

	/**
	 * The externally owned caller contract that pays for the transaction.
	 */
	public final StorageReference caller;

	/**
	 * The gas provided to the transaction.
	 */
	public final BigInteger gas;

	/**
	 * The class path that specifies where the {@code caller} should be interpreted.
	 */
	public final Classpath classpath;

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
	 * @param gas the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 */
	public JarStoreTransactionRequest(StorageReference caller, BigInteger gas, Classpath classpath, byte[] jar, Classpath... dependencies) {
		this.caller = caller;
		this.gas = gas;
		this.classpath = classpath;
		this.jar = jar.clone();
		this.dependencies = dependencies;
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

	/**
	 * Yields the size of the jar to install (in bytes).
	 * 
	 * @return the size of the jar to install
	 */
	public int getJarSize() {
		return jar.length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        for (byte b: jar)
            sb.append(String.format("%02x", b));

        return getClass().getSimpleName() + ":\n"
        	+ "  caller: " + caller + "\n"
        	+ "  gas: " + gas + "\n"
        	+ "  class path: " + classpath + "\n"
			+ "  dependencies: " + Arrays.toString(dependencies) + "\n"
			+ "  jar: " + sb.toString();
	}

	@Override
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(GasCosts.STORAGE_COST_PER_SLOT)
			.add(caller.size()).add(GasCosts.storageCostOf(gas)).add(classpath.size())
			.add(getDependencies().map(Classpath::size).reduce(BigInteger.ZERO, BigInteger::add))
			.add(GasCosts.storageCostForInstalling(jar));
	}

	@Override
	public boolean hasMinimalGas(UpdateOfBalance balanceUpdateInCaseOfFailure) {
		// we create a response whose size over-approximates that of a response in case of failure of this request
		return gas.compareTo(GasCosts.BASE_CPU_TRANSACTION_COST.add(size()).add(new JarStoreTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas).size())) >= 0;
	}
}