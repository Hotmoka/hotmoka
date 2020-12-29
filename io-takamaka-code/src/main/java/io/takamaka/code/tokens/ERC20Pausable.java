package io.takamaka.code.tokens;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.auxiliaries.IPausable;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.UnsignedBigInteger;

/**
 * Implementation inspired by OpenZeppelin:
 * https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/ERC20Pausable.sol
 *
 * OpenZeppelin: Useful for scenarios such as preventing trades until the end of an evaluation period, or having an
 *  emergency switch for freezing all token transfers in the event of a large bug.
 */
public abstract class ERC20Pausable extends ERC20 implements IPausable {
    // represents the paused state of the contract
    private boolean _paused;

    /**
     * OpenZeppelin: Sets the values for {name} and {symbol}, initializes {decimals} with a default value of 18.
     * To select a different value for {decimals}, use {_setupDecimals}.
     * All three of these values are immutable: they can only be set once during construction.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     */
    public ERC20Pausable(String name, String symbol) {
        super(name, symbol);
        _paused = false;
    }

    /**
     * See {IPausable-paused}.
     */
    public final @View boolean paused() {
        return _paused;
    }

    /**
     * OpenZeppelin: Triggers stopped state.
     *
     * Requirements:
     * - The contract must not be paused.
     *
     * @param caller the account requesting the pause
     */
    protected void _pause(Contract caller) {
        require(!_paused, "Pausable: paused");

        _paused = true;
        event(new Paused(caller));
    }

    /**
     * OpenZeppelin: Returns to normal state.
     *
     * Requirements:
     * - The contract must be paused.
     *
     * @param caller the account which removed the pause
     */
    protected void _unpause(Contract caller) {
        require(_paused, "Pausable: not paused");

        _paused = false;
        event(new Unpaused(caller));
    }

    /**
     * OpenZeppelin: See {ERC20-_beforeTokenTransfer}.
     *
     * Requirements:
     * - the contract must not be paused.
     *
     * @param from token transfer source account
     * @param to token transfer recipient account
     * @param amount amount of tokens transferred
     */
    @Override
    protected void _beforeTokenTransfer(Contract from, Contract to, UnsignedBigInteger amount) {
        super._beforeTokenTransfer(from, to, amount);

        require(!paused(), "ERC20Pausable: token transfer while paused");
    }
}
