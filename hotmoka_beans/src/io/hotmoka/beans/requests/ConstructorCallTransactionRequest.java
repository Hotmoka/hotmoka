package io.hotmoka.beans.requests;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A request for calling a constructor of a storage class in blockchain.
 */
@Immutable
public class ConstructorCallTransactionRequest extends CodeExecutionTransactionRequest<ConstructorCallTransactionResponse> {

	private static final long serialVersionUID = -6485399240275200765L;

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
		super(caller, gas, classpath);

		this.constructor = constructor;
		this.actuals = actuals;
	}

	/**
	 * Yields the actual arguments passed to the constructor.
	 * 
	 * @return the actual arguments
	 */
	public final Stream<StorageValue> actuals() {
		return Stream.of(actuals);
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  caller: " + caller + "\n"
        	+ "  gas: " + gas + "\n"
        	+ "  class path: " + classpath + "\n"
			+ "  constructor: " + constructor + "\n"
			+ "  actuals:\n" + actuals().map(StorageValue::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}
}