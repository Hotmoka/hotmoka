package io.takamaka.code.lang;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

/**
 * A contract that can be paused.
 * Implementation inspired by OpenZeppelin's <a href="https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/utils/Pausable.sol">Pausable.sol</a>.
 *
 * See {@link Pausable}.
 */
public class PausableContract extends Contract implements Pausable {
	
	/**
	 * True if and only if the contract is paused.
	 */
    private boolean paused;

    /**
     * Creates the contract, initially unpaused.
     */
    public PausableContract() {}

    @Override
    public final @View boolean paused() {
        return paused;
    }

    @Override
    public @FromContract void pause() {
        require(!paused, "the contract is already paused");
        paused = true;
        event(new Paused(caller()));
    }

    @Override
    public @FromContract void unpause() {
        require(paused, "the contract is not paused at the moment");
        paused = false;
        event(new Unpaused(caller()));
    }
}