package io.takamaka.code.tokens;

import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.UnsignedBigInteger;

import java.math.BigInteger;

/**
 * Implementation inspired by OpenZeppelin:
 * https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/ERC20Capped.sol
 *
 * OpenZeppelin: Extension of {ERC20} that adds a cap to the supply of tokens.
 */
public abstract class ERC20Capped extends ERC20{
    // The cap to the supply of tokens
    private final UnsignedBigInteger _cap;

    /**
     * OpenZeppelin: Sets the values for {name} and {symbol}, initializes {decimals} with a default value of 18.
     * To select a different value for {decimals}, use {_setupDecimals}.
     * All three of these values are immutable: they can only be set once during construction.
     *
     * Sets the value of the `cap`. This value is immutable, it can only be set once during construction.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     * @param cap the cap to the supply of tokens
     */
    public ERC20Capped(String name, String symbol, UnsignedBigInteger cap) {
        super(name, symbol);

        require(!cap.equals(new UnsignedBigInteger(BigInteger.ZERO)), "ERC20Capped: cap is 0");
        _cap = cap;
    }

    /**
     * OpenZeppelin: Returns the cap on the token's total supply.
     *
     * @return the cap on the token's total supply
     */
    public final @View UnsignedBigInteger cap() {
        return _cap;
    }

    /**
     * OpenZeppelin: See {ERC20-_beforeTokenTransfer}.
     *
     * Requirements:
     * - minted tokens must not cause the total supply to go over the cap.
     *
     * @param from token transfer source account
     * @param to token transfer recipient account
     * @param amount amount of tokens transferred
     */
    @Override
    protected void _beforeTokenTransfer(Contract from, Contract to, UnsignedBigInteger amount) {
        super._beforeTokenTransfer(from, to, amount);

        if (from == null) // When minting tokens
            require(totalSupply().add(amount).compareTo(_cap) <= 0, "ERC20Capped: cap exceeded");
    }
}
