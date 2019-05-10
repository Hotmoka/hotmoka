package takamaka.tests.collections;

import java.math.BigInteger;

import takamaka.lang.Storage;
import takamaka.util.StorageMap;
import takamaka.util.StorageMap.Entry;

/**
 * This class defines methods that test the storage map implementation.
 */
public class MapTests extends Storage {

	public static int testIteration1() {
		StorageMap<BigInteger, BigInteger> map = new StorageMap<>();
		for (BigInteger key = BigInteger.ZERO; key.intValue() < 100; key = key.add(BigInteger.ONE))
			map.put(key, key);

		return map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue).sum();
	}
}