package io.takamaka.code.auxiliaries;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * Implementation inspired by OpenZeppelin:
 * https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/utils/Pausable.sol
 *
 * See {IPausable}.
 */
public class Pausable extends Contract implements IPausable {
    // represents the paused state of the contract
    private boolean _paused;

    /**
     * OpenZeppelin: Initializes the contract in unpaused state.
     */
    public Pausable() {
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
}
