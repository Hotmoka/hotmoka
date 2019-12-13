package io.takamaka.code.blockchain.request;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.ConstructorSignature;
import io.takamaka.code.blockchain.GasCostModel;
import io.takamaka.code.blockchain.UpdateOfBalance;
import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.response.ConstructorCallTransactionFailedResponse;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StorageValue;

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
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot())
			.add(BigInteger.valueOf(gasCostModel.storageCostPerSlot())).add(caller.size(gasCostModel))
			.add(gasCostModel.storageCostOf(gas)).add(classpath.size(gasCostModel))
			.add(Stream.of(actuals).map(value -> value.size(gasCostModel)).reduce(BigInteger.ZERO, BigInteger::add));
	}

	@Override
	public boolean hasMinimalGas(UpdateOfBalance balanceUpdateInCaseOfFailure, GasCostModel gasCostModel) {
		// we create a response whose size over-approximates that of a response in case of failure of this request
		return gas.compareTo(BigInteger.valueOf(gasCostModel.cpuBaseTransactionCost()).add(size(gasCostModel)).add(new ConstructorCallTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas).size(gasCostModel))) >= 0;
	}
}