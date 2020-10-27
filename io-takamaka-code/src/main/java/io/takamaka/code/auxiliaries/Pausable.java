package io.takamaka.code.auxiliaries;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
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
     */
    protected @Entry void pause() {
        require(!_paused, "Pausable: paused");

        _paused = true;
        event(new Paused(this, caller()));
    }

    /**
     * OpenZeppelin: Returns to normal state.
     *
     * Requirements:
     * - The contract must be paused.
     */
    protected @Entry void unpause() {
        require(_paused, "Pausable: not paused");

        _paused = false;
        event(new Unpaused(this, caller()));
    }
}
