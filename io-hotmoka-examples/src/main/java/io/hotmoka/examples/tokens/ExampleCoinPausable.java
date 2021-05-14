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

package io.hotmoka.examples.tokens;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.tokens.ERC20Pausable;

import static io.takamaka.code.lang.Takamaka.require;

/**
 * A token example that use Pausable extension of ERC20 standard implementation.
 * The owner (deployer) of the contract can put or remove the contract from the paused state
 */
public class ExampleCoinPausable extends ERC20Pausable {
    private final Contract owner;

    /**
     * Sets the initial settings of the coin
     */
    public @FromContract ExampleCoinPausable() {
        super("ExampleCoinPausable", "EXCP");

        owner = caller();
        setDecimals((short) 18); // redundant, just for example

        UnsignedBigInteger initial_EXCP_supply = new UnsignedBigInteger("200000");
        UnsignedBigInteger multiplier = new UnsignedBigInteger("10").pow(18);
        _mint(caller(), initial_EXCP_supply.multiply(multiplier)); // 200'000 EXCP = 200'000 * 10 ^ 18 MiniEp
    }

    @Override
    public @FromContract void pause() {
        require(caller() == owner, "Lack of permission");
        super.pause();
    }

    @Override
    public @FromContract void unpause() {
        require(caller() == owner, "Lack of permission");
        super.unpause();
    }
}