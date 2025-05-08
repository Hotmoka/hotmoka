/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.moka.api;

import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;

/**
 * The gas cost incurred for the some execution.
 */
@Immutable
public interface GasCost {

	/**
	 * Yields the gas consumed for CPU.
	 * 
	 * @return the gas computed for CPU
	 */
	BigInteger getForCPU();

	/**
	 * Yields the gas consumed for RAM.
	 * 
	 * @return the gas computed for RAM
	 */
	BigInteger getForRAM();

	/**
	 * Yields the gas consumed for storage.
	 * 
	 * @return the gas computed for storage
	 */
	BigInteger getForStorage();

	/**
	 * Yields the gas consumed for penalty.
	 * 
	 * @return the gas computed for penalty
	 */
	BigInteger getForPenalty();

	/**
	 * Yields the price of a unit of gas used for the execution.
	 * 
	 * @return the price of a unit of gas used for the execution
	 */
	BigInteger getPrice();

	/**
	 * Reports this gas cost information inside the given string builder.
	 * 
	 * @param sb the string builder
	 */
	void toString(StringBuilder sb);
}