package io.takamaka.tests.collections;

import java.math.BigInteger;

import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageMap.Entry;

/**
 * This class defines methods that test the storage map implementation.
 */
public class MapTests {

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