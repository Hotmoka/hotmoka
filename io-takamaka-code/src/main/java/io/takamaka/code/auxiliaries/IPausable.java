package io.takamaka.code.auxiliaries;

import io.takamaka.code.lang.*;

/**
 * Implementation inspired by OpenZeppelin:
 * https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/utils/Pausable.sol
 *
 * OpenZeppelin: Contract module which allows children to implement an emergency stop mechanism that can be triggered
 *  by an authorized account. This module is used through inheritance.
 */
public interface IPausable {
    /**
     * OpenZeppelin: Returns true if the contract is paused, and false otherwise.
     *
     * @return true if the contract is paused, and false otherwise.
     */
    public @View boolean paused();

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
     * OpenZeppelin: Emitted when the pause is triggered by `account`.
     */
    public static class Paused extends Event {
        public final Contract account;

        /**
         * Allows the Paused event to be issued.
         *
         * @param account the account requesting the pause
         */
        public @FromContract Paused(Contract account) { //TODO public? it is safe?
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
         * @param account the account which removed the pause
         */
        public @FromContract Unpaused(Contract account) { //TODO public? it is safe?
            this.account = account;
        }
    }
}
