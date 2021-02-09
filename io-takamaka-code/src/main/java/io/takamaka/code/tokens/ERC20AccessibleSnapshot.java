package io.takamaka.code.tokens;

import io.takamaka.code.auxiliaries.Counter;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

import java.math.BigInteger;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

/**
 * Extension of {@link ERC20} which extends the functionality of Takamaka native screenshots
 * (see {@link ERC20#snapshot()}) and makes them searchable, similar to the implementation made by OpenZeppelin.
 * Each snapshot is identified by an id.
 */
public class ERC20AccessibleSnapshot extends ERC20 {
    private final StorageMap<UnsignedBigInteger, IERC20View> _snapshots = new StorageTreeMap<>();
    private final Counter _currentSnapshotId = new Counter(); // Note: First snapshot has the id 1 -> see snapshot()

    /**
     * Sets the values for {@code name} and {@code symbol}, initializes {@code _decimals} with a default
     * value of 18. To select a different value for {@code _decimals}, use {@link ERC20#_setupDecimals(short)}.
     * All three of these values are immutable: they can only be set once during construction.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     */
    public @FromContract ERC20AccessibleSnapshot(String name, String symbol) {
        super(name, symbol);
    }

    /**
     * Emitted when a snapshot identified by {@code id} is created.
     */
    public static class Snapshot extends Event {
        public final UnsignedBigInteger id;

        /**
         * Allows the {@link ERC20AccessibleSnapshot.Snapshot} event to be issued.
         *
         * @param id the id of the created snapshot
         */
        @FromContract Snapshot(UnsignedBigInteger id) {
            this.id = id;
        }
    }

    /**
     * Returns the id of the last screenshot that was created.
     *
     * @return the id of the last screenshot
     */
    public final @View UnsignedBigInteger getCurrentSnapshotId() {
        return _currentSnapshotId.current();
    }

    /**
     * Creates a new snapshot and returns its snapshot id.
     * Emits a {@link ERC20AccessibleSnapshot.Snapshot} event that contains the same id.
     *
     * @return id of the created snapshot
     */
    @Override
    public @FromContract IERC20View snapshot() {
        IERC20View snapshot = super.snapshot();

        _currentSnapshotId.increment();
        UnsignedBigInteger currentId = _currentSnapshotId.current();
        _snapshots.put(currentId, snapshot);

        event(new Snapshot(currentId));

        return snapshot;
    }

    /**
     * Retrieves the balance of {@code account} at the time the snapshot {@code snapshotId} was created.
     *
     * @param account account whose balance is to be retrieved
     * @param snapshotId snapshot from which to recover the balance
     * @return the balance of {@code account} relative to the snapshot {@code snapshotId}
     */
    public final @View UnsignedBigInteger balanceOfAt(Contract account, UnsignedBigInteger snapshotId) {
        return _getSnapshot(snapshotId).balanceOf(account);
    }

    /**
     * Retrieves the total supply at the time the snapshot {@code snapshotId} was created.
     *
     * @param snapshotId snapshot from which to recover the total supply
     * @return the total supply relative to the snapshot {@code snapshotId}
     */
    public final @View UnsignedBigInteger totalSupplyAt(UnsignedBigInteger snapshotId) {
        return _getSnapshot(snapshotId).totalSupply();
    }

    /**
     * Returns the snapshot with the identifier {@code snapshotId}
     *
     * @param snapshotId identifier of the snapshot to be returned
     * @return the snapshot with the identifier {@code snapshotId}
     */
    private @View IERC20View _getSnapshot(UnsignedBigInteger snapshotId) {
        require(snapshotId != null, "Id cannot be null");
        require(snapshotId.compareTo(new UnsignedBigInteger(BigInteger.ZERO)) > 0, "Id cannot be 0");
        require(snapshotId.compareTo(_currentSnapshotId.current()) <= 0, "Nonexistent id");

        return _snapshots.getOrDefault(snapshotId, this);
    }
}
