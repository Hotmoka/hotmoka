/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.instrumentation.api;

import java.math.BigInteger;

/**
 * A specification of the cost of gas.
 */
public interface GasCostModel {

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
	 * Yields the CPU gas cost for installing in store a jar consisting of the given number of bytes.
	 * 
	 * @param numBytes the number of bytes of the jar
	 * @return the cost
	 */
	BigInteger cpuCostForInstallingJar(int numBytes);

	/**
	 * Yields the CPU gas cost for loading from store a jar consisting of the given number of bytes.
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
	 * Yields the storage gas cost for storing a byte in the database. This applies
	 * to requests and responses, that get stored into the database of a node.
	 * 
	 * @return the cost
	 */
	BigInteger storageCostOfByte();
}