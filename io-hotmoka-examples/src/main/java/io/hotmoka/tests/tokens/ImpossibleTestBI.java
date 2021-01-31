package io.hotmoka.tests.tokens;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.tokens.IERC20View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

import java.math.BigInteger;


public class ImpossibleTestBI extends Storage {
    private final StorageMap<BigInteger, IERC20View> _snapshots = new StorageTreeMap<>();
    private final BigInteger one = new BigInteger("1");

    public ImpossibleTestBI() {
        _snapshots.put(one, null);
    }

    public final @View boolean test(BigInteger snapshotId) {
        return _snapshots.keyList().size() == 1 &&
                _snapshots.keyList().get(0).equals(one) &&
                _snapshots.keyList().get(0).equals(snapshotId) &&
                one.equals(snapshotId) &&
                !_snapshots.containsKey(snapshotId);
    }
}
