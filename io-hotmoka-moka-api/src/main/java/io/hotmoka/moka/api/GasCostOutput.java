/*
Copyright 2025 Fausto Spoto

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
 * The output of a command that reports its gas cost.
 */
@Immutable
public interface GasCostOutput {

	/**
	 * Yields the amount of gas consumed for the CPU cost for installing the jar.
	 * 
	 * @return the amount of gas consumed for the CPU cost for installing the jar
	 */
	BigInteger getGasConsumedForCPU();

	/**
	 * Yields the amount of gas consumed for the RAM cost for installing the jar.
	 * 
	 * @return the amount of gas consumed for the RAM cost for installing the jar
	 */
	BigInteger getGasConsumedForRAM();

	/**
	 * Yields the amount of gas consumed for the storage cost for installing the jar.
	 * 
	 * @return the amount of gas consumed for the storage cost for installing the jar
	 */
	BigInteger getGasConsumedForStorage();

	/**
	 * The gas price used for the transaction.
	 * 
	 * @return the gas price used for the transaction
	 */
	BigInteger getGasPrice();
}