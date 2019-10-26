package takamaka.blockchain.request;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.takamaka.code.annotations.Immutable;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.GasCosts;
import takamaka.blockchain.UpdateOfBalance;
import takamaka.blockchain.response.ConstructorCallTransactionFailedResponse;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;

/**
 * A request for calling a constructor of a storage class in blockchain.
 */
@Immutable
public class ConstructorCallTransactionRequest implements TransactionRequest {

	private static final long serialVersionUID = -6485399240275200765L;

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
	 * The constructor to call.
	 */
	public final ConstructorSignature constructor;

	/**
	 * The actual arguments passed to the constructor.
	 */
	private final StorageValue[] actuals;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gas the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 */
	public ConstructorCallTransactionRequest(StorageReference caller, BigInteger gas, Classpath classpath, ConstructorSignature constructor, StorageValue... actuals) {
		this.caller = caller;
		this.gas = gas;
		this.classpath = classpath;
		this.constructor = constructor;
		this.actuals = actuals;
	}

	/**
	 * Yields the actual arguments passed to the constructor.
	 * 
	 * @return the actual arguments
	 */
	public Stream<StorageValue> getActuals() {
		return Stream.of(actuals);
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  caller: " + caller + "\n"
        	+ "  gas: " + gas + "\n"
        	+ "  class path: " + classpath + "\n"
			+ "  constructor: " + constructor + "\n"
			+ "  actuals:\n" + getActuals().map(StorageValue::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(GasCosts.STORAGE_COST_PER_SLOT).add(caller.size()).add(GasCosts.storageCostOf(gas)).add(classpath.size())
				.add(Stream.of(actuals).map(StorageValue::size).reduce(BigInteger.ZERO, BigInteger::add));
	}

	@Override
	public boolean hasMinimalGas(UpdateOfBalance balanceUpdateInCaseOfFailure) {
		// we create a response whose size over-approximates that of a response in case of failure of this request
		return gas.compareTo(GasCosts.BASE_CPU_TRANSACTION_COST.add(size()).add(new ConstructorCallTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas).size())) >= 0;
	}
}