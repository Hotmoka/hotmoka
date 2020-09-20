package io.takamaka.tests.tokens;

import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.tokens.ERC20;
import io.takamaka.code.util.UnsignedBigInteger;

/**
 * A token example that use ERC20 standard implementation
 *
 * We call our coin "ExampleCoin" and its symbol is "EXC".
 * "MiniEx" is the smallest subunit of the coin, where 10^18 MiniEx are an EXC.
 * The Total Supply value is decided when the token is created and can change during the the life of the contract.
 * The owner (deployer) of the contract can mint or burn tokens.
 */
public class ExampleCoin extends ERC20 {
    private final Contract owner;

    /**
     * Sets the initial settings of the coin
     */
    public @Entry ExampleCoin() {
        super("ExampleCoin", "EXC");

        owner = caller();
        _setupDecimals((short) 18); // redundant, just for example

        UnsignedBigInteger initial_EXC_supply = new UnsignedBigInteger("200000");
        UnsignedBigInteger multiplier = new UnsignedBigInteger("10").pow(18);
        _mint(caller(), initial_EXC_supply.multiply(multiplier)); // 200'000 EXC = 200'000 * 10 ^ 18 MiniEx
    }

    /**
     * Mint tokens
     *
     * @param account recipient of the created tokens
     * @param amount number of tokens to create
     */
    public @Entry void mint(Contract account, UnsignedBigInteger amount) {
        require(caller().equals(owner), "Lack of permission");
        _mint(account, amount);
    }

    /**
     * Burn tokens
     *
     * @param account source of tokens to burn
     * @param amount number of tokens to burn
     */
    public @Entry void burn(Contract account, UnsignedBigInteger amount) {
        require(caller().equals(owner), "Lack of permission");
        _burn(account, amount);
    }
}




