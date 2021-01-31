package io.hotmoka.tests.tokens;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.tokens.IERC20View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;


public class ImpossibleTestUBI extends Storage {
    private final StorageMap<UnsignedBigInteger, IERC20View> _snapshots = new StorageTreeMap<>();
    private final UnsignedBigInteger one = new UnsignedBigInteger("1");

    public ImpossibleTestUBI() {
        _snapshots.put(one, null);
    }

    public final @View boolean test(UnsignedBigInteger snapshotId) {
        return _snapshots.keyList().size() == 1 &&
                _snapshots.keyList().get(0).equals(one) &&
                _snapshots.keyList().get(0).equals(snapshotId) &&
                one.equals(snapshotId) &&
                !_snapshots.containsKey(snapshotId);
    }
}
