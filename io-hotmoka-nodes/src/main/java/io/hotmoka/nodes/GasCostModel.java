package io.hotmoka.nodes;

import java.math.BigInteger;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.nodes.internal.StandardGasCostModel;

/**
 * A specification of the cost of gas.
 */
public interface GasCostModel {

	/**
	 * Yields a cost model that provides standard measurements for gas consumption.
	 * 
	 * @return the standard cost model
	 */
	static GasCostModel standard() {
		return new StandardGasCostModel();
	}

	/**
	 * Yields the RAM cost of an object, without considering its fields.
	 * Hence, this is a constant, not depending on the size of the object.
	 * 
	 * @return the cost
	 */
	int ramCostOfObject();

	/**
	 * Yields the RAM cost of a single field of an object allocated in memory. This does not consider the
	 * objects reachable from the field, if any.
	 * 
	 * @return the cost
	 */
	int ramCostOfField();

	/**
	 * Yields the RAM cost of a single element of an array.
	 * 
	 * @return the cost
	 */
	int ramCostOfArraySlot();

	/**
	 * Yields the RAM cost of an array, without considering its elements.
	 * Hence, this is a constant, not depending on the length of the array.
	 * 
	 * @return the cost
	 */
	int ramCostOfArray();

	/**
	 * Yields the RAM cost of an activation record, without considering the
	 * variables therein. Hence, this is a constant, not depending on
	 * the number of local variables inside the activation record.
	 * 
	 * @return the cost
	 */
	int ramCostOfActivationRecord();

	/**
	 * Yields the RAM cost of a single variable inside an activation record.
	 * 
	 * @return the cost
	 */
	int ramCostOfActivationSlot();

	/**
	 * Yields the RAM gas cost for installing in store a jar consisting of the given bytes.
	 * 
	 * @param numBytes the number of bytes of the jar
	 * @return the cost
	 */
	BigInteger ramCostForInstallingJar(int numBytes);

	/**
	 * Yields the RAM gas cost for loading from store a jar consisting of the given bytes.
	 * 
	 * @param numBytes the number of bytes of the jar
	 * @return the cost
	 */
	BigInteger ramCostForLoadingJar(int numBytes);

	/**
	 * Yields the CPU gas cost of the execution of an arithmetic instruction.
	 * 
	 * @return the cost
	 */
	int cpuCostOfArithmeticInstruction();

	/**
	 * Yields the CPU gas cost of the execution of an array access instruction.
	 * 
	 * @return the cost
	 */
	int cpuCostOfArrayAccessInstruction();

	/**
	 * Yields the CPU gas cost of the execution of a field access instruction.
	 * 
	 * @return the cost
	 */
	int cpuCostOfFieldAccessInstruction();

	/**
	 * Yields the CPU gas cost of the execution of an invoke instruction.
	 * 
	 * @return the cost
	 */
	int cpuCostOfInvokeInstruction();

	/**
	 * Yields the CPU gas cost of the execution of a select instruction.
	 * 
	 * @return the cost
	 */
	int cpuCostOfSelectInstruction();

	/**
	 * Yields the CPU gas cost of the execution of a memory allocation instruction.
	 * This is a constant, that does not consider the size of the allocated
	 * object. That size is charged instead in terms of RAM allocation, through instrumentation code.
	 * 
	 * @return the cost
	 */
	int cpuCostOfMemoryAllocationInstruction();

	/**
	 * Yields the CPU gas cost of the execution of an instruction that does not fall
	 * in any more specific category.
	 * 
	 * @return the cost
	 */
	int cpuCostOfInstruction();

	/**
	 * Yields the CPU gas cost for installing in store a jar consisting of the given bytes.
	 * 
	 * @param numBytes the number of bytes of the jar
	 * @return the cost
	 */
	BigInteger cpuCostForInstallingJar(int numBytes);

	/**
	 * Yields the CPU gas cost for loading from store a jar consisting of the given bytes.
	 * 
	 * @param numBytes the number of bytes of the jar
	 * @return the cost
	 */
	BigInteger cpuCostForLoadingJar(int numBytes);

	/**
	 * Yields the CPU gas cost for starting the execution of a transaction.
	 * 
	 * @return the cost
	 */
	BigInteger cpuBaseTransactionCost();

	/**
	 * Yields the CPU gas cost for accessing from store the response at the given transaction.
	 * 
	 * @param transaction the transaction
	 * @return the cost
	 */
	BigInteger cpuCostForGettingResponseAt(TransactionReference transaction);

	/**
	 * Yields the storage gas cost for a variable inside a data structure.
	 * It does not consider what is reachable from that variable but only the slot itself.
	 * 
	 * @return the cost
	 */
	int storageCostPerSlot();

	/**
	 * Yields the storage gas cost for a slot containing the given value.
	 * It considers the value as well.
	 * 
	 * @param value the value
	 * @return the cost
	 */
	BigInteger storageCostOf(BigInteger value);

	/**
	 * Yields the storage gas cost for a slot containing the given value.
	 * It considers the value as well.
	 * 
	 * @param value the value
	 * @return the cost
	 */
	BigInteger storageCostOf(String value);

	/**
	 * Yields the storage gas cost for installing in store a jar consisting of the given bytes.
	 * 
	 * @param numBytes the number of bytes of the jar
	 * @return the cost
	 */
	BigInteger storageCostOfJar(int numBytes);

	/**
	 * Yields the storage gas cost for the given transaction reference, if stored in store.
	 * 
	 * @param transaction the transaction reference
	 * @return the cost
	 */
	BigInteger storageCostOf(TransactionReference transaction);
}