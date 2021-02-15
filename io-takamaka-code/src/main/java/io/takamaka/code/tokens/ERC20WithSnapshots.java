package io.takamaka.code.tokens;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

/**
 * An {@link IERC20} token decorator, that additionally tracks
 * the snapshots performed and makes them accessible by progressive numbers.
 * This mimics the implementation by OpenZeppelin.
 */
public abstract class ERC20WithSnapshots extends Contract implements IERC20 {
	protected final IERC20 parent;
    private final StorageMap<UnsignedBigInteger, IERC20View> _snapshots = new StorageTreeMap<>();
    private UnsignedBigInteger _currentSnapshotId = new UnsignedBigInteger(); // Note: First snapshot has the id 1 -> see snapshot()

    /**
     * Sets the values for {@code name} and {@code symbol}, initializes {@code _decimals} with a default
     * value of 18. To select a different value for {@code _decimals}, use {@link ERC20#_setupDecimals(short)}.
     * All three of these values are immutable: they can only be set once during construction.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     */
    public @FromContract ERC20WithSnapshots(IERC20 parent) {
    	this.parent = parent;
    }

    /**
     * Emitted when a snapshot identified by {@code id} is created.
     */
    public static class Snapshot extends Event {
        public final UnsignedBigInteger id;

        /**
         * Allows the {@link ERC20WithSnapshots.Snapshot} event to be issued.
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
        return _currentSnapshotId;
    }

    @Override @FromContract
	public final boolean transfer(Contract recipient, int amount) {
    	return transfer(recipient, new UnsignedBigInteger(amount));
	}

	@Override @FromContract
	public final boolean transfer(Contract recipient, long amount) {
		return transfer(recipient, new UnsignedBigInteger(amount));
	}

    /**
     * Creates a new snapshot and returns its snapshot id.
     * Emits a {@link ERC20WithSnapshots.Snapshot} event that contains the same id.
     *
     * @return id of the created snapshot
     */
    @Override
    public @FromContract IERC20View snapshot() {
        IERC20View snapshot = parent.snapshot();

        _currentSnapshotId = _currentSnapshotId.next();
        _snapshots.put(_currentSnapshotId, snapshot);

        event(new Snapshot(_currentSnapshotId));

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
        require(snapshotId != null, "the id cannot be null");
        require(snapshotId.signum() > 0, "the id cannot be 0");
        require(snapshotId.compareTo(_currentSnapshotId) <= 0, "non-existent id");

        return _snapshots.getOrDefault(snapshotId, this);
    }

	@Override
	public @View UnsignedBigInteger totalSupply() {
		return parent.totalSupply();
	}

	@Override
	public @View UnsignedBigInteger balanceOf(Contract account) {
		return parent.balanceOf(account);
	}

	@Override
	public @View UnsignedBigInteger allowance(Contract owner, Contract spender) {
		return parent.allowance(owner, spender);
	}
}