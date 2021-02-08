package io.hotmoka.tests.collections;

import static java.math.BigInteger.ONE;

import java.math.BigInteger;

import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageIntMap;
import io.takamaka.code.util.StorageTreeIntMap;
import io.takamaka.code.util.StorageIntMapView.Entry;

/**
 * This class defines methods that test the storage map with integer keys implementation.
 */
public class IntMapTests {

	public static @View int testIteration1() {
		StorageIntMap<BigInteger> map = new StorageTreeIntMap<>();
		for (int key = 0; key < 100; key++)
			map.put(key, BigInteger.valueOf(key));

		return map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue).sum();
	}

	public static @View int testUpdate1() {
		StorageIntMap<BigInteger> map = new StorageTreeIntMap<>();
		for (int key = 0; key < 100; key++)
			map.put(key, BigInteger.valueOf(key));

		// we add one to the value bound to each key
		map.keyList().forEach(key -> map.update(key, ONE::add));

		return map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue).sum();
	}

	public static @View int testUpdate2() {
		StorageIntMap<BigInteger> map = new StorageTreeIntMap<>();
		for (int key = 0; key < 100; key++)
			map.put(key, BigInteger.valueOf(key));

		// we add one to the value bound to each key
		map.keys().forEachOrdered(key -> map.update(key, ONE::add));

		return map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue).sum();
	}

	public static @View long testNullValues() {
		StorageIntMap<BigInteger> map = new StorageTreeIntMap<>();
		for (int key = 0; key < 100; key++)
			map.put(key, null);

		return map.stream().map(Entry::getValue).filter(value -> value == null).count();
	}
}