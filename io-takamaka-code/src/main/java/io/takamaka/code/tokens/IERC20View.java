/*
Copyright 2021 Marco Crosara and Fausto Spoto

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

package io.takamaka.code.tokens;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.UnsignedBigInteger;

/**
 * The read operations on the balance and total supply of an ERC20 token.
 */
public interface IERC20View {

	/**
     * Yields the amount of tokens in existence.
     *
     * @return the amount of tokens in existence
     */
	@View UnsignedBigInteger totalSupply();

    /**
     * Yields the amount of tokens owned by {@code account}.
     *
     * @param account account whose balance you want to check
     * @return the amount of tokens owned by {@code account}
     */
	@View UnsignedBigInteger balanceOf(Contract account);

	/**
	 * Returns the number of owners of tokens. This includes also owners of
	 * zero tokens, but does not include contracts that have never been owners.
	 * 
	 * @return the number of owners of tokens
	 */
	@View int size();

	/**
	 * Yields the k-th owner of tokens.
	 * This is the k-th contract in the token table. 
	 *
	 * @param k the rank of the owner, between 0 and {@link #size()}
	 * @return the {@code k}-th owner of tokens
	 * @throws IllegalArgumentException if {@code k} is not between 0 and {@code size()-1}
	 */
	@View Contract select(int k);

	/**
	 * Yields a snapshot of this ERC20 token. The snapshot is an immutable
	 * view of the current total supply and balances.
	 * 
	 * @return the snapshot
	 */
	@View IERC20View snapshot();
}