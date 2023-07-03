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

package io.hotmoka.helpers.api;

import java.math.BigInteger;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;

/**
 * An object that helps with gas operations.
 */
public interface GasHelper {

	/**
	 * Yields the gas price for a transaction.
	 * 
	 * @return the gas price
	 */
	BigInteger getGasPrice() throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Yields a safe gas price for a transaction, that should be valid
	 * for a little time, also in case of small changes in the gas price.
	 * This is simply the double of {@link #getGasPrice()}.
	 * 
	 * @return a safe gas price
	 */
	BigInteger getSafeGasPrice() throws TransactionRejectedException, TransactionException, CodeExecutionException;
}