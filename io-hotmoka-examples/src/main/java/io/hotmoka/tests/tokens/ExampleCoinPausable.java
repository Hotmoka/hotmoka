package io.hotmoka.tests.tokens;

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
        _setupDecimals((short) 18); // redundant, just for example

        UnsignedBigInteger initial_EXCP_supply = new UnsignedBigInteger("200000");
        UnsignedBigInteger multiplier = new UnsignedBigInteger("10").pow(18);
        _mint(caller(), initial_EXCP_supply.multiply(multiplier)); // 200'000 EXCP = 200'000 * 10 ^ 18 MiniEp
    }

    /**
     * Puts the contract in the paused state
     */
    public @FromContract void pause() {
        require(caller() == owner, "Lack of permission");
        _pause(caller());
    }

    /**
     * Removes the contract from the paused state
     */
    public @FromContract void unpause() {
        require(caller() == owner, "Lack of permission");
        _unpause(caller());
    }
}
