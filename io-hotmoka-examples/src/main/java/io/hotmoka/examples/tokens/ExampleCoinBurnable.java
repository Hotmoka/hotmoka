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

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.tokens.ERC20Burnable;

/**
 * A token example that use Burnable extension of ERC20 standard implementation.
 */
public class ExampleCoinBurnable  extends ERC20Burnable {
    /**
     * Sets the initial settings of the coin
     */
    public @FromContract ExampleCoinBurnable() {
        super("ExampleCoinBurnable", "EXCB");

        setDecimals((short) 18); // redundant, just for example

        UnsignedBigInteger initial_EXCB_supply = new UnsignedBigInteger("200000");
        UnsignedBigInteger multiplier = new UnsignedBigInteger("10").pow(18);
        _mint(caller(), initial_EXCB_supply.multiply(multiplier)); // 200'000 EXCB = 200'000 * 10 ^ 18 MiniEb
    }
}
