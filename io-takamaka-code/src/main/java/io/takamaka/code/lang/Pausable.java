/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.takamaka.code.lang;

/**
 * Implementation inspired by OpenZeppelin's <a href="https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/utils/Pausable.sol">Pausable.sol</a>.
 * Allows subclasses to implement an emergency stop mechanism that can be triggered by an authorized account.
 * 
 * Java interfaces do not allow one to define protected methods. For this reason, it is not
 * possible to define a generic implementation of this interface, with default methods.
 */
public interface Pausable {

	/**
     * Returns true if the object is paused, and false otherwise.
     *
     * @return true if the object is paused, and false otherwise.
     */
    @View boolean paused();

    /**
     * Puts the objects in pause.
     * Requirement: the object must not be paused.
     */
    @FromContract void pause();

    /**
     * Unpauses the object.
     * Requirement: the contract must be paused.
     */
    @FromContract void unpause();

    /**
     * Emitted when a contract is paused.
     */
    class Paused extends Event {

    	/**
    	 * The paused contract.
    	 */
    	public final Contract contract;

        /**
         * Creates the event object.
         *
         * @param contract the contract that has been paused
         */
        public @FromContract Paused(Contract contract) {
            this.contract = contract;
        }
    }

    /**
     * Emitted when the pause is lifted by {@code account}.
     */
    class Unpaused extends Event {

    	/**
    	 * The unpaused contract.
    	 */
    	public final Contract contract;

        /**
         * Creates the event.
         *
         * @param contract the contract that has been unpaused
         */
        public @FromContract Unpaused(Contract contract) {
            this.contract = contract;
        }
    }
}