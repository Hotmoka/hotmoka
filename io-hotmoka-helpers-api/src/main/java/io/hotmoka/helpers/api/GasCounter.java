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

package io.hotmoka.helpers.api;

import java.math.BigInteger;

import io.hotmoka.annotations.ThreadSafe;

/**
 * A counter of the gas consumed for the execution of a set of requests.
 */
@ThreadSafe
public interface GasCounter {

	/**
	 * Yields the total gas consumed.
	 * 
	 * @return the total gas computed
	 */
	BigInteger total();

	/**
	 * Yields the gas consumed for CPU.
	 * 
	 * @return the gas computed for CPU
	 */
	BigInteger forCPU();

	/**
	 * Yields the gas consumed for RAM.
	 * 
	 * @return the gas computed for RAM
	 */
	BigInteger forRAM();

	/**
	 * Yields the gas consumed for storage.
	 * 
	 * @return the gas computed for storage
	 */
	BigInteger forStorage();

	/**
	 * Yields the gas consumed for penalty.
	 * 
	 * @return the gas computed for penalty
	 */
	BigInteger forPenalty();
}