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

public class ERC20AccessibleSnapshot extends ERC20 {
    private final StorageMap<UnsignedBigInteger, IERC20View> _snapshots = new StorageTreeMap<>();
    private final Counter _currentSnapshotId = new Counter();

    /**
     * OpenZeppelin: Sets the values for {@code name} and {@code symbol}, initializes {@code _decimals} with a default
     *  value of 18. To select a different value for {@code _decimals}, use {@link #_setupDecimals(short)}. All three of
     *  these values are immutable: they can only be set once during construction.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     */
    public @FromContract ERC20AccessibleSnapshot(String name, String symbol) {
        super(name, symbol);
    }

    /**
     * OpenZeppelin: Emitted when a snapshot identified by {@code id} is created.
     */
    public static class Snapshot extends Event {
        public final UnsignedBigInteger id;

        /**
         * Allows the {@link ERC20AccessibleSnapshot.Snapshot} event to be issued.
         *
         * @param id the id of the created snapshot
         */
        @FromContract
        Snapshot(UnsignedBigInteger id) {
            this.id = id;
        }
    }

    public final @View UnsignedBigInteger getCurrentSnapshotId() {
        return _currentSnapshotId.current();
    }

    @Override
    public @FromContract IERC20View snapshot() {
        IERC20View snapshot = super.snapshot();

        _currentSnapshotId.increment(); // TODO NB riflettere su vulnerabilità poichè ora è pubblica
        UnsignedBigInteger currentId = _currentSnapshotId.current();
        _snapshots.put(currentId, snapshot);

        event(new Snapshot(currentId));

        return snapshot;
    }

    public final @View UnsignedBigInteger balanceOfAt(Contract account, UnsignedBigInteger snapshotId) {
        return _getSnapshot(snapshotId).balanceOf(account);
    }

    public final @View UnsignedBigInteger totalSupplyAt(UnsignedBigInteger snapshotId) {
        return _getSnapshot(snapshotId).totalSupply();
    }

    private @View IERC20View _getSnapshot(UnsignedBigInteger snapshotId) {
        require(snapshotId != null, "Id cannot be null");
        require(snapshotId.compareTo(new UnsignedBigInteger(BigInteger.ZERO)) > 0, "Id cannot be 0");
        require(snapshotId.compareTo(_currentSnapshotId.current()) <= 0, "Nonexistent id");
        // controlla che snapshotId non sia null
        // se snapshoId non esiste (troppo alto) ritorna il valore attuale
        return _snapshots.getOrDefault(snapshotId, this);
    }
}
