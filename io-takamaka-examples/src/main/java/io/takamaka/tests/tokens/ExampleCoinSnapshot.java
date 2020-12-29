package io.takamaka.tests.tokens;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.tokens.ERC20Snapshot;
import io.takamaka.code.util.UnsignedBigInteger;

import static io.takamaka.code.lang.Takamaka.require;

/**
 * A token example that use Snapshot extension of ERC20 standard implementation.
 * The owner (deployer) of the contract can create a new snapshot, mint tokens and burn tokens.
 */
public class ExampleCoinSnapshot extends ERC20Snapshot {
    private final Contract owner;

    /**
     * Sets the initial settings of the coin
     */
    public @FromContract ExampleCoinSnapshot() {
        super("ExampleCoinSnapshot", "EXCS");

        owner = caller();
        _setupDecimals((short) 18); // redundant, just for example

        UnsignedBigInteger initial_EXCS_supply = new UnsignedBigInteger("200000");
        UnsignedBigInteger multiplier = new UnsignedBigInteger("10").pow(18);
        _mint(caller(), initial_EXCS_supply.multiply(multiplier)); // 200'000 EXCS = 200'000 * 10 ^ 18 MiniEs
    }

    /**
     * Creates a new snapshot and returns its snapshot id
     *
     * @return snapshot id
     */
    public @FromContract UnsignedBigInteger snapshot() {
        require(caller() == owner, "Lack of permission");
        return _snapshot();
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
