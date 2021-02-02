package io.takamaka.code.tokens;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.auxiliaries.Counter;
import io.takamaka.code.auxiliaries.Pair;
import io.takamaka.code.lang.*;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.util.*;

import java.math.BigInteger;

/**
 * Implementation inspired by OpenZeppelin:
 * https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/ERC20Snapshot.sol
 *
 * OpenZeppelin: This contract extends an ERC20 token with a snapshot mechanism. When a snapshot is created, the
 *  balances and total supply at the time are recorded for later access.
 *
 *  This can be used to safely create mechanisms based on token balances such as trustless dividends or weighted voting.
 *  In naive implementations it's possible to perform a "double spend" attack by reusing the same balance from different
 *  accounts. By using snapshots to calculate dividends or voting power, those attacks no longer apply. It can also be
 *  used to create an efficient ERC20 forking mechanism.
 *
 *  Snapshots are created by the internal {_snapshot} function, which will emit the {Snapshot} event and return a
 *  snapshot id. To get the total supply at the time of a snapshot, call the function {totalSupplyAt} with the snapshot
 *  id. To get the balance of an account at the time of a snapshot, call the {balanceOfAt} function with the snapshot id
 *  and the account address.
 *
 *  ==== Gas Costs
 *
 *  Snapshots are efficient. Snapshot creation is _O(1)_. Retrieval of balances or total supply from a snapshot is _O(lo
 *  gn)_ in the number of snapshots that have been created, although _n_ for a specific account will generally be much
 *  smaller since identical balances in subsequent snapshots are stored as a single entry.
 *
 *  There is a constant overhead for normal ERC20 transfers due to the additional snapshot bookkeeping. This overhead is
 *  only significant for the first transfer that immediately follows a snapshot for a particular account. Subsequent
 *  transfers will have normal cost until the next snapshot, and so on.
 */
public class ERC20OZSnapshot extends ERC20 {
    /**
     * Snapshotted values have lists of ids and the value corresponding to that id. These could be an list of a
     * Snapshot struct, but that would impede usage of functions that work on an list.
     */
    public static class Snapshots extends Storage{
        StorageList<UnsignedBigInteger> ids = new StorageLinkedList<>();
        StorageList<UnsignedBigInteger> values = new StorageLinkedList<>();
    }

    private final StorageMap<Contract, Snapshots> _accountBalanceSnapshots = new StorageTreeMap<>();
    private final Snapshots _totalSupplySnapshots = new Snapshots();

    // Snapshot ids increase monotonically, with the first value being 1. An id of 0 is invalid.
    private final Counter _currentSnapshotId = new Counter();

    /**
     * OpenZeppelin: Sets the values for {name} and {symbol}, initializes {decimals} with a default value of 18.
     * To select a different value for {decimals}, use {_setupDecimals}.
     * All three of these values are immutable: they can only be set once during construction.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     */
    public ERC20OZSnapshot(String name, String symbol) {
        super(name, symbol);
    }

    /**
     * OpenZeppelin: Emitted by {_snapshot} when a snapshot identified by `id` is created.
     */
    public static class Snapshot extends Event {
        public final UnsignedBigInteger id;

        /**
         * Allows the Snapshot event to be issued.
         *
         * @param id the id of the created snapshot
         */
        @FromContract Snapshot(UnsignedBigInteger id) {
            this.id = id;
        }
    }

    /**
     * OpenZeppelin: Creates a new snapshot and returns its snapshot id.
     *
     * Emits a {Snapshot} event that contains the same id.
     *
     * {_snapshot} is `internal` and you have to decide how to expose it externally. Its usage may be restricted to a
     * set of accounts, for example using {AccessControl}, or it may be open to the public.
     *
     * [WARNING]
     * ====
     * While an open way of calling {_snapshot} is required for certain trust minimization mechanisms such as forking,
     * you must consider that it can potentially be used by attackers in two ways.
     *
     * First, it can be used to increase the cost of retrieval of values from snapshots, although it will grow
     * logarithmically thus rendering this attack ineffective in the long term. Second, it can be used to target
     * specific accounts and increase the cost of ERC20 transfers for them, in the ways specified in the Gas Costs
     * section above.
     *
     * We haven't measured the actual numbers; if this is something you're interested in please reach out to us.
     * ====
     *
     * @return id of the created snapshot
     */
    protected UnsignedBigInteger _snapshot() {
        _currentSnapshotId.increment();

        UnsignedBigInteger currentId = _currentSnapshotId.current();
        event(new Snapshot(currentId));
        return currentId;
    }

    /**
     * OpenZeppelin: Retrieves the balance of `account` at the time `snapshotId` was created.
     *
     * @param account account whose balance is to be retrieved
     * @param snapshotId snapshot from which to recover the balance
     * @return the balance of `account` at the time `snapshotId`
     */
    public final @View UnsignedBigInteger balanceOfAt(Contract account, UnsignedBigInteger snapshotId) {
        Pair<Boolean, UnsignedBigInteger> pair = _valueAt(snapshotId,
                _accountBalanceSnapshots.getOrDefault(account, Snapshots::new));
        // pair.first = snapshotted, pair.second = value

        return pair.first ? pair.second : balanceOf(account);
    }

    /**
     * OpenZeppelin: Retrieves the total supply at the time `snapshotId` was created.
     *
     * @param snapshotId snapshot from which to recover the total supply
     * @return the total supply at the time `snapshotId`
     */
    public final @View UnsignedBigInteger totalSupplyAt(UnsignedBigInteger snapshotId) {
        Pair<Boolean, UnsignedBigInteger> pair = _valueAt(snapshotId, _totalSupplySnapshots);
        // pair.first = snapshotted, pair.second = value

        return pair.first ? pair.second : totalSupply();
    }

    /**
     * Updates balance and/or total supply snapshots before the values are modified. This is implemented in the
     * _beforeTokenTransfer hook, which is executed for _mint, _burn, and _transfer operations.
     *
     * @param from token transfer source account (null if mint)
     * @param to token transfer recipient account (null if burn)
     * @param amount amount of tokens transferred
     */
    @Override
    protected void _beforeTokenTransfer(Contract from, Contract to, UnsignedBigInteger amount) {
        super._beforeTokenTransfer(from, to, amount);

        if (from == null) { // mint
            _updateAccountSnapshot(to);
            _updateTotalSupplySnapshot();
        } else if (to == null) { // burn
            _updateAccountSnapshot(from);
            _updateTotalSupplySnapshot();
        } else { // transfer
            _updateAccountSnapshot(from);
            _updateAccountSnapshot(to);
        }
    }

    /**
     * Consults a `snapshots` to get (if any) the token value (balance or total supply) at a certain `snapshotId`.
     *
     * @param snapshotId snapshot from which to recover the token value
     * @param snapshots snapshots to consult
     * @return a pair - true if exist a token value for `snapshotId` in `snapshots`, false otherwise
     *                - token value for `snapshotId` in `snapshots`
     */
    private @View Pair<Boolean, UnsignedBigInteger> _valueAt(UnsignedBigInteger snapshotId, Snapshots snapshots) {
        require(snapshotId.compareTo(new UnsignedBigInteger(BigInteger.ZERO)) > 0,
                "ERC20Snapshot: id is 0");
        require(snapshotId.compareTo(_currentSnapshotId.current()) <= 0,
                "ERC20Snapshot: nonexistent id");

        // When a valid snapshot is queried, there are three possibilities:
        //  a) The queried value was not modified after the snapshot was taken. Therefore, a snapshot entry was never
        //  created for this id, and all stored snapshot ids are smaller than the requested one. The value that
        //  corresponds to this id is the current one.
        //  b) The queried value was modified after the snapshot was taken. Therefore, there will be an entry with the
        //  requested id, and its value is the one to return.
        //  c) More snapshots were created after the requested one, and the queried value was later modified. There will
        //  be no entry for the requested id: the value that corresponds to it is that of the smallest snapshot id that
        //  is larger than the requested one.
        //
        // In summary, we need to find an element in an list, returning the index of the smallest value that is larger
        // if it is not found, unless said value doesn't exist (e.g. when all values are smaller).
        // findUpperBound does exactly this.
        int index = findUpperBound(snapshots.ids, snapshotId);

        if (index == snapshots.ids.size())
            return new Pair<>(false, new UnsignedBigInteger(BigInteger.ZERO));
        else
            return new Pair<>(true, snapshots.values.get(index));
    }

    /**
     * Updates an account snapshots following a transfer, mint or burn operation
     *
     * @param account account whose snapshot is to be updated
     */
    private void _updateAccountSnapshot(Contract account) {
        _accountBalanceSnapshots.computeIfAbsent(account, Snapshots::new);
        _updateSnapshot(_accountBalanceSnapshots.get(account), balanceOf(account));
    }

    /**
     *  Updates the total supply snapshots following a mint or burn operation
     */
    private void _updateTotalSupplySnapshot() {
        _updateSnapshot(_totalSupplySnapshots, totalSupply());
    }

    /**
     * Updates a `snapshots` with the current token value, following a transfer, mint or burn operation
     *
     * @param snapshots snapshots to update
     * @param currentValue current token value to be added in `snapshots`
     */
    private void _updateSnapshot(Snapshots snapshots, UnsignedBigInteger currentValue) {
        UnsignedBigInteger currentId = _currentSnapshotId.current();
        if (_lastSnapshotId(snapshots.ids).compareTo(currentId) < 0) {
            snapshots.ids.add(currentId);
            snapshots.values.add(currentValue);
        }
    }

    /**
     * Returns the last id of a given `ids` list. If the list is empty it returns zero.
     *
     * @param ids `ids` list where to look for the last id
     * @return the last id of the `ids` list
     */
    private @View UnsignedBigInteger _lastSnapshotId(StorageList<UnsignedBigInteger> ids) {
        if (ids.size() == 0)
            return new UnsignedBigInteger(BigInteger.ZERO);
        else
            return ids.get(ids.size() - 1);
    }

    /**
     * Implementation inspired by OpenZeppelin:
     * https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/utils/Arrays.sol
     *
     * OpenZeppelin: Searches a sorted `list` and returns the first index that contains a value greater or equal to
     * `element`. If no such index exists (i.e. all values in the list are strictly less than `element`), the list
     * length is returned. Time complexity O(log n).
     *
     * `list` is expected to be sorted in ascending order, and to contain no repeated elements.
     *
     * @param list the list in which to search for the `element`
     * @param element the item to search for
     * @return the first index in the `list` that contains a value greater or equal to `element`
     */
    private static @View int findUpperBound(StorageList<UnsignedBigInteger> list, UnsignedBigInteger element) {
        if (list.size() == 0)
            return 0;

        int low = 0;
        int high = list.size();

        while (low < high) {
            int mid = (low+high)/2;

            // Note that mid will always be strictly less than high (i.e. it will be a valid list index)
            // because the average made above rounds down (it does integer division with truncation).
            if (list.get(mid).compareTo(element) > 0)
                high = mid;
            else
                low = mid + 1;
        }

        // At this point `low` is the exclusive upper bound. We will return the inclusive upper bound.
        if (low > 0 && list.get(low - 1).equals(element))
            return low - 1;
        else
            return low;
    }
}
