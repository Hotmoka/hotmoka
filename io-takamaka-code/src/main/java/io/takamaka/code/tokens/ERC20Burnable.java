package io.takamaka.code.tokens;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.util.UnsignedBigInteger;

/**
 * Implementation inspired by OpenZeppelin:
 * https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/ERC20Burnable.sol
 *
 * OpenZeppelin: Extension of {ERC20} that allows token holders to destroy both their own tokens and those that they
 *  have an allowance for, in a way that can be recognized off-chain (via event analysis).
 */
public class ERC20Burnable extends ERC20{

    /**
     * OpenZeppelin: Sets the values for {name} and {symbol}, initializes {decimals} with a default value of 18.
     * To select a different value for {decimals}, use {_setupDecimals}.
     * All three of these values are immutable: they can only be set once during construction.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     */
    public ERC20Burnable(String name, String symbol) {
        super(name, symbol);
    }

    /**
     * OpenZeppelin: Destroys `amount` tokens from the caller.
     *
     * See {ERC20-_burn}.
     *
     * @param amount number of tokens to burn (it cannot be null)
     */
    public @Entry void burn(UnsignedBigInteger amount) {
        _burn(caller(), amount);
    }

    /**
     * OpenZeppelin: Destroys `amount` tokens from `account`, deducting from the caller's allowance.
     *
     * See {ERC20-_burn} and {ERC20-allowance}.
     *
     * Requirements:
     * - the caller must have allowance for ``accounts``'s tokens of at least `amount`.
     *
     * @param account account in which to burn tokens (it cannot be the null account, it must have a balance of at
     *                least `amount`)
     * @param amount number of tokens to burn (it cannot be null)
     */
    public @Entry void burnFrom(Contract account, UnsignedBigInteger amount) {
        UnsignedBigInteger decreasedAllowance = allowance(account, caller())
                .subtract(amount, "ERC20: burn amount exceeds allowance");

        _approve(account, caller(), decreasedAllowance);
        _burn(account, amount);
    }
}
