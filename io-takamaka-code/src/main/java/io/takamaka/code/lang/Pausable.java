package io.takamaka.code.lang;

/**
 * Implementation inspired by OpenZeppelin's <a href="https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/utils/Pausable.sol">Pausable.sol</a>.
 *
 * OpenZeppelin: module that allows children to implement an emergency stop mechanism that can be triggered
 * by an authorized account. This module is used through inheritance.
 * 
 * Java interfaces do not allow one to define protected methods. For this reason, it is not
 * possible to define a generic implementation of this interface, with default methods.
 * Implementations can copy the example code in class {@link PausableContract}.
 */
public interface Pausable {
    /**
     * OpenZeppelin: Returns true if the contract is paused, and false otherwise.
     *
     * @return true if the contract is paused, and false otherwise.
     */
    @View boolean paused();

    /*
     * OpenZeppelin: Triggers stopped state.
     *
     * Requirements:
     * - The contract must not be paused.
     */
    // protected void _pause();

    /*
     * OpenZeppelin: Returns to normal state.
     *
     * Requirements:
     * - The contract must be paused.
     */
    // protected void _unpause();

    /**
     * OpenZeppelin: Emitted when the pause is triggered by {@code account}.
     *
     * The constructor is public for implementation reasons.
     * This is not a problem because the events emitted are nominal.
     */
    class Paused extends Event {
        public final Contract account;

        /**
         * Allows the {@link Pausable.Paused} event to be issued.
         *
         * @param account the account requesting the pause
         */
        public @FromContract Paused(Contract account) {
            this.account = account;
        }
    }

    /**
     * OpenZeppelin: Emitted when the pause is lifted by {@code account}.
     *
     * The constructor is public for implementation reasons.
     * This is not a problem because the events emitted are nominal.
     *
     */
    class Unpaused extends Event {
        public final Contract account;

        /**
         * Allows the {@link Pausable.Unpaused} event to be issued.
         *
         * @param account the account which removed the pause
         */
        public @FromContract Unpaused(Contract account) {
            this.account = account;
        }
    }
}
