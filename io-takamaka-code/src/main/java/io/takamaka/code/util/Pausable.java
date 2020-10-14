package io.takamaka.code.util;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.View;

/**
 * Implementation inspired by OpenZeppelin:
 * https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/utils/Pausable.sol
 *
 * OpenZeppelin: Contract module which allows children to implement an emergency stop mechanism that can be triggered
 *  by an authorized account. This module is used through inheritance. It will make available the modifiers
 *  `whenNotPaused` and `whenPaused`, which can be applied to the functions of your contract.
 *  Note that they will not be pausable by simply including this module, only once the modifiers are put in place.
 */
public class Pausable extends Contract {
    private boolean _paused; // TODO is it necessary to replace the notation with the java one? paused, this.paused, this.paused = paused

    /**
     * OpenZeppelin: Initializes the contract in unpaused state.
     */
    protected Pausable() { //TODO NB protected
        _paused = false;
    }

    /**
     * OpenZeppelin: Returns true if the contract is paused, and false otherwise.
     *
     * @return true if the contract is paused, and false otherwise.
     */
    public final @View boolean paused() {
        return _paused;
    }

    //TODO Modifiers??
    /**
     * OpenZeppelin: Modifier to make a function callable only when the contract is not paused.
     *
     * Requirements:
     * - The contract must not be paused.
     */
    /*modifier whenNotPaused() {
        require(!_paused, "Pausable: paused");
        _;
    }*/

    /**
     * OpenZeppelin: Modifier to make a function callable only when the contract is paused.
     *
     * Requirements:
     * - The contract must be paused.
     */
    /*modifier whenPaused() {
        require(_paused, "Pausable: not paused");
        _;
    }*/

    /**
     * OpenZeppelin: Triggers stopped state.
     *
     * Requirements:
     * - The contract must not be paused.
     */
    protected @Entry void _pause() /*whenNotPaused*/{
        require(!_paused, "Pausable: paused"); //TODO substitute modifier checks

        _paused = true;
        event(new Paused(this, caller()));
    }

    /**
     * OpenZeppelin: Returns to normal state.
     *
     * Requirements:
     * - The contract must be paused.
     */
    protected @Entry void _unpause() /*whenPaused*/{
        require(_paused, "Pausable: not paused"); //TODO substitute modifier checks

        _paused = false;
        event(new Unpaused(this, caller()));
    }

    /**
     * OpenZeppelin: Emitted when the pause is triggered by `account`.
     */
    public static class Paused extends Event {
        public final Contract account;

        /**
         * Allows the Paused event to be issued.
         *
         * @param key the key of the event
         * @param account the account requesting the pause
         */
        Paused(Contract key, Contract account) {
            super(key);

            this.account = account;
        }
    }

    /**
     * OpenZeppelin: Emitted when the pause is lifted by `account`.
     */
    public static class Unpaused extends Event {
        public final Contract account;

        /**
         * Allows the Unpaused event to be issued.
         *
         * @param key the key of the event
         * @param account the account which removed the pause
         */
        Unpaused(Contract key, Contract account) {
            super(key);

            this.account = account;
        }
    }
}
