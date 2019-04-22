package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.Set;

import takamaka.blockchain.Update;
import takamaka.blockchain.values.StorageReference;
import takamaka.lang.Immutable;

/**
 * A response for a successful transaction that calls a constructor of a storage
 * class in blockchain. The constructor has been called without problems and
 * without generating exceptions.
 */
@Immutable
public class ConstructorCallTransactionSuccessfulResponse extends ConstructorCallTransactionResponse {

	private static final long serialVersionUID = -7514325398187177242L;

	/**
	 * The object that has been created by the constructor call.
	 */
	public final StorageReference newObject;

	/**
	 * Builds the transaction response.
	 * 
	 * @param newObject the object that has been successfully created
	 * @param updates the updates resulting from the execution of the transaction
	 * @param consumedGas the amount of gas consumed by the transaction
	 */
	public ConstructorCallTransactionSuccessfulResponse(StorageReference newObject, Set<Update> updates, BigInteger consumedGas) {
		super(updates, consumedGas);

		this.newObject = newObject;
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
        	+ "  new object: " + newObject;
	}
}