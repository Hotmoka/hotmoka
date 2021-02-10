package io.takamaka.code.lang;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

/**
 * A contract that has a paused state.
 * Implementation inspired by OpenZeppelin's <a href="https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/utils/Pausable.sol">Pausable.sol</a>
 *
 * See {@link Pausable}.
 */
public class PausableContract extends Contract implements Pausable {
    // represents the paused state of the contract
    private boolean _paused;

    /**
     * OpenZeppelin: Initializes the contract in unpaused state.
     */
    public PausableContract() {
        _paused = false;
    }

    @Override
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
        require(!_paused, "the contract is already paused");

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
        require(_paused, "the contract is not paused at the moment");

        _paused = false;
        event(new Unpaused(caller));
    }
}