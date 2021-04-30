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