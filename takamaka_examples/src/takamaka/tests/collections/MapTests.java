package takamaka.tests.collections;

import java.math.BigInteger;

import takamaka.lang.Storage;
import takamaka.lang.View;
import takamaka.util.StorageMap;
import takamaka.util.StorageMap.Entry;

/**
 * This class defines methods that test the storage map implementation.
 */
public class MapTests extends Storage {

	public static @View int testIteration1() {
		StorageMap<BigInteger, BigInteger> map = new StorageMap<>();
		for (BigInteger key = BigInteger.ZERO; key.intValue() < 100; key = key.add(BigInteger.ONE))
			map.put(key, key);

		return map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue).sum();
	}

	public static @View int testUpdate1() {
		StorageMap<BigInteger, BigInteger> map = new StorageMap<>();
		for (BigInteger key = BigInteger.ZERO; key.intValue() < 100; key = key.add(BigInteger.ONE))
			map.put(key, key);

		// we add one to the value bound to each key
		map.keyList().forEach(key -> map.update(key, BigInteger.ONE::add));

		return map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue).sum();
	}

	public static @View int testUpdate2() {
		StorageMap<BigInteger, BigInteger> map = new StorageMap<>();
		for (BigInteger key = BigInteger.ZERO; key.intValue() < 100; key = key.add(BigInteger.ONE))
			map.put(key, key);

		// we add one to the value bound to each key
		map.keys().forEachOrdered(key -> map.update(key, BigInteger.ONE::add));

		return map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue).sum();
	}

	public static @View long testNullValues() {
		StorageMap<BigInteger, BigInteger> map = new StorageMap<>();
		for (BigInteger key = BigInteger.ZERO; key.intValue() < 100; key = key.add(BigInteger.ONE))
			map.put(key, null);

		return map.stream().map(Entry::getValue).filter(value -> value == null).count();
	}
}