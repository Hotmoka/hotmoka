package io.takamaka.tests.tokens;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.tokens.ERC20;
import io.takamaka.code.util.UnsignedBigInteger;

/**
 * A token example that use ERC20 standard implementation
 *
 * We call our coin "ExampleCoin" and its symbol is "EXC".
 * "MiniEx" is the smallest subunit of the coin, where 10^18 MiniEx are an EXC.
 * The Total Supply value is decided when the token is created and cannot be changed later.
 */
public class ExampleCoin extends ERC20 {

    /**
     * Sets the initial settings of the coin
     */
    public @Entry ExampleCoin() {
        super("ExampleCoin", "EXC");

        _setupDecimals((short) 18); // redundant, just for example

        UnsignedBigInteger initial_EXC_supply = new UnsignedBigInteger("200000");
        UnsignedBigInteger multiplier = new UnsignedBigInteger("10").pow(18);
        _mint(caller(), initial_EXC_supply.multiply(multiplier)); // 200'000 EXC = 200'000 * 10 ^ 18 MiniEx
    }
}




