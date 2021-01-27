package io.hotmoka.tests.tokens;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.tokens.ERC20Capped;

import static io.takamaka.code.lang.Takamaka.require;

/**
 * A token example that use Capped extension of ERC20 standard implementation.
 * The owner (deployer) of the contract can mint or burn tokens.
 */
public class ExampleCoinCapped extends ERC20Capped {
    private final Contract owner;

    /**
     * Sets the initial settings of the coin
     */
    public @FromContract ExampleCoinCapped() {
        super("ExampleCoinCapped", "EXCC",
                new UnsignedBigInteger("1000000").multiply(new UnsignedBigInteger("10").pow(18))); // 1Million EXCC

        owner = caller();
        _setupDecimals((short) 18); // redundant, just for example

        UnsignedBigInteger initial_EXCC_supply = new UnsignedBigInteger("200000");
        UnsignedBigInteger multiplier = new UnsignedBigInteger("10").pow(18);
        _mint(caller(), initial_EXCC_supply.multiply(multiplier)); // 200'000 EXCC = 200'000 * 10 ^ 18 MiniEc
    }

    /**
     * Mint tokens
     *
     * @param account recipient of the created tokens
     * @param amount number of tokens to create
     */
    public @FromContract void mint(Contract account, UnsignedBigInteger amount) {
        require(caller() == owner, "Lack of permission");
        _mint(account, amount);
    }

    /**
     * Burn tokens
     *
     * @param account source of tokens to burn
     * @param amount number of tokens to burn
     */
    public @FromContract void burn(Contract account, UnsignedBigInteger amount) {
        require(caller() == owner, "Lack of permission");
        _burn(account, amount);
    }
}
