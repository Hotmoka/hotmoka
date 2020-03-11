package io.takamaka.tests.collections;

import java.math.BigInteger;
import java.util.Random;

import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageArray;

/**
 * This class defines methods that test the storage array implementation.
 */
public class ArrayTests {

	public static @View int testRandomInitialization() {
		StorageArray<BigInteger> array = new StorageArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(100), BigInteger.valueOf(i)) != null);

		return array.stream().filter(value -> value != null).mapToInt(BigInteger::intValue).sum();
	}

	public static @View long countNullsAfterRandomInitialization() {
		StorageArray<BigInteger> array = new StorageArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(100), BigInteger.valueOf(i)) != null);

		// 50 elements of the array should still be null
		return array.stream().filter(value -> value == null).count();
	}

	public static @View int testUpdateWithDefault1() {
		StorageArray<BigInteger> array = new StorageArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(array.length), BigInteger.valueOf(i)) != null);

		for (int i = 0; i < array.length; i++)
			array.update(i, BigInteger.ZERO, BigInteger.ONE::add);

		return array.stream().mapToInt(BigInteger::intValue).sum();
	}

	public static @View int testUpdateWithDefault2() {
		StorageArray<BigInteger> array = new StorageArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			array.update(random.nextInt(100), BigInteger.ZERO, BigInteger.valueOf(i)::add);

		return array.stream().filter(bi -> bi != null).mapToInt(BigInteger::intValue).sum();
	}

	public static @View int testGetOrDefault() {
		StorageArray<BigInteger> array = new StorageArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(array.length), BigInteger.valueOf(i)) != null);

		BigInteger sum = BigInteger.ZERO;
		for (int i = 0; i < array.length; i++)
			sum = sum.add(array.getOrDefault(i, BigInteger.ZERO));

		return sum.intValue();
	}
}