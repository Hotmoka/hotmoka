package io.hotmoka.tests.tokens;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.tokens.ERC20AccessibleSnapshot;
import io.takamaka.code.tokens.IERC20View;

import static io.takamaka.code.lang.Takamaka.require;

/**
 * A token example that use Snapshot extension of ERC20 standard implementation.
 * The owner (deployer) of the contract can create a new snapshot, mint tokens and burn tokens.
 */
public class ExampleCoinAccessibleSnapshot extends ERC20AccessibleSnapshot {
    private final Contract owner;

    /**
     * Sets the initial settings of the coin
     */
    public @FromContract ExampleCoinAccessibleSnapshot() {
        super("ExampleCoinAccessibleSnapshot", "EXCAS");

        owner = caller();
        _setupDecimals((short) 18); // redundant, just for example

        UnsignedBigInteger initial_EXCS_supply = new UnsignedBigInteger("200000");
        UnsignedBigInteger multiplier = new UnsignedBigInteger("10").pow(18);
        _mint(caller(), initial_EXCS_supply.multiply(multiplier)); // 200'000 EXCS = 200'000 * 10 ^ 18 MiniEs
    }

    /**
     * Creates a new snapshot and returns its snapshot id
     * Note: In this example we have chosen to allow only the owner to take snapshots
     *
     * TODO commenta
     *
     * @return snapshot id
     */
    public @FromContract UnsignedBigInteger yieldSnapshot() {
        snapshot();
        return getCurrentSnapshotId();
    }

    /**
     * TODO commenta
     * @return
     */
    @Override
    public @FromContract IERC20View snapshot() {
        require(caller() == owner, "Lack of permission");
        return super.snapshot();
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
