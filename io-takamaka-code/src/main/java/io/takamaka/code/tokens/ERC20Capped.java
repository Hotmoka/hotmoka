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

package io.takamaka.code.tokens;

import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.UnsignedBigInteger;

/**
 * Implementation inspired by OpenZeppelin's <a href="https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/ERC20Capped.sol">ERC20Capped.sol</a>
 * Extension of {@link ERC20} that adds a cap to the supply of tokens.
 */
public abstract class ERC20Capped extends ERC20{

	/**
	 * The cap to the supply of tokens.
	 */
    private final UnsignedBigInteger cap;

    /**
     * Sets the values for {@code name} and {@code symbol}, initializes {@code decimals} with a default
     * value of 18. To select a different value for {@code decimals}, use {@link ERC20#setDecimals(short)}.
     * All three of these values are immutable: they can only be set once during construction.
     *
     * Sets the value of the {@code cap}. This value is immutable, it can only be set once during construction.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     * @param cap the cap to the supply of tokens
     */
    public ERC20Capped(String name, String symbol, UnsignedBigInteger cap) {
        super(name, symbol);

        require(cap.signum() > 0, "ERC20Capped: cap is 0");
        this.cap = cap;
    }

    /**
     * Returns the cap on the token's total supply.
     *
     * @return the cap on the token's total supply
     */
    public final @View UnsignedBigInteger cap() {
        return cap;
    }

    /**
     * See {@link ERC20#beforeTokenTransfer(Contract, Contract, UnsignedBigInteger)}.
     * Requirement: minted tokens must not cause the total supply to go over the cap.
     *
     * @param from token transfer source account
     * @param to token transfer recipient account
     * @param amount amount of tokens transferred
     */
    @Override
    protected void beforeTokenTransfer(Contract from, Contract to, UnsignedBigInteger amount) {
        super.beforeTokenTransfer(from, to, amount);

        if (from == null) // When minting tokens
            require(totalSupply().add(amount).compareTo(cap) <= 0, "ERC20Capped: cap exceeded");
    }
}