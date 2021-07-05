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
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.math.UnsignedBigInteger;

/**
 * Implementation inspired by OpenZeppelin's <a href="https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/ERC20Burnable.sol">ERC20Burnable.sol</a>
 *
 * Extension of {@link ERC20} that allows token holders to destroy both their own tokens and those that
 * they have an allowance for, in a way that can be recognized off-chain (via event analysis).
 */
public abstract class ERC20Burnable extends ERC20 {

	/**
     * Sets the values for {@code name} and {@code symbol}, initializes {@code decimals} with a default
     * value of 18. To select a different value for {@code decimals}, use {@link ERC20#setDecimals(short)}.
     * All three of these values are immutable: they can only be set once during construction.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     */
    public ERC20Burnable(String name, String symbol) {
        super(name, symbol);
    }

    /**
     * Destroys {@code amount} tokens from the caller.
     * See {@link ERC20#_burn(Contract, UnsignedBigInteger)}.
     *
     * @param amount number of tokens to burn (it cannot be null)
     */
    public @FromContract void burn(UnsignedBigInteger amount) {
        _burn(caller(), amount);
    }

    /**
     * Destroys {@code amount} tokens from {@code account}, deducting from the caller's allowance.
     * See {@link ERC20#_burn(Contract, UnsignedBigInteger)} and {@link ERC20#allowance(Contract, Contract)}.
     *
     * Requirement: the caller must have allowance for {@code account}'s tokens of at least {@code amount}.
     *
     * @param account account in which to burn tokens (it cannot be the null account, it must have a balance of at least {@code amount})
     * @param amount number of tokens to burn (it cannot be null)
     */
    public @FromContract void burnFrom(Contract account, UnsignedBigInteger amount) {
        UnsignedBigInteger decreasedAllowance = allowance(account, caller()).subtract(amount, "ERC20: burn amount exceeds allowance");
        _approve(account, caller(), decreasedAllowance);
        _burn(account, amount);
    }
}