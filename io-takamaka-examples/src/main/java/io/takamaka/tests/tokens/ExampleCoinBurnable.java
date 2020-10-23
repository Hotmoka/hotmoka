package io.takamaka.tests.tokens;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.tokens.ERC20Burnable;
import io.takamaka.code.util.UnsignedBigInteger;

/**
 * A token example that use Burnable extension of ERC20 standard implementation.
 */
public class ExampleCoinBurnable  extends ERC20Burnable {
    /**
     * Sets the initial settings of the coin
     */
    public @Entry ExampleCoinBurnable() {
        super("ExampleCoinBurnable", "EXCB");

        _setupDecimals((short) 18); // redundant, just for example

        UnsignedBigInteger initial_EXC_supply = new UnsignedBigInteger("200000");
        UnsignedBigInteger multiplier = new UnsignedBigInteger("10").pow(18);
        _mint(caller(), initial_EXC_supply.multiply(multiplier)); // 200'000 EXCB = 200'000 * 10 ^ 18 MiniEb
    }
}
