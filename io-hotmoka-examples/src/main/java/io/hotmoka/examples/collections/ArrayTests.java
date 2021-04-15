package io.hotmoka.examples.collections;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;

import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageTreeArray;
import io.takamaka.code.util.StorageTreeByteArray;

/**
 * This class defines methods that test the storage array implementation.
 */
public class ArrayTests {

	public static @View int testRandomInitialization() {
		StorageTreeArray<BigInteger> array = new StorageTreeArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(100), BigInteger.valueOf(i)) != null);

		return array.stream().filter(Objects::nonNull).mapToInt(BigInteger::intValue).sum();
	}

	public static @View long countNullsAfterRandomInitialization() {
		StorageTreeArray<BigInteger> array = new StorageTreeArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(100), BigInteger.valueOf(i)) != null);

		// 50 elements of the array should still be null
		return array.stream().filter(Objects::isNull).count();
	}

	public static @View int testUpdateWithDefault1() {
		StorageTreeArray<BigInteger> array = new StorageTreeArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(array.length), BigInteger.valueOf(i)) != null);

		for (int i = 0; i < array.length; i++)
			array.update(i, BigInteger.ZERO, BigInteger.ONE::add);

		return array.stream().mapToInt(BigInteger::intValue).sum();
	}

	public static @View int testUpdateWithDefault2() {
		StorageTreeArray<BigInteger> array = new StorageTreeArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			array.update(random.nextInt(100), BigInteger.ZERO, BigInteger.valueOf(i)::add);

		return array.stream().filter(Objects::nonNull).mapToInt(BigInteger::intValue).sum();
	}

	public static @View int testGetOrDefault() {
		StorageTreeArray<BigInteger> array = new StorageTreeArray<>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(array.length), BigInteger.valueOf(i)) != null);

		BigInteger sum = BigInteger.ZERO;
		for (int i = 0; i < array.length; i++)
			sum = sum.add(array.getOrDefault(i, BigInteger.ZERO));

		return sum.intValue();
	}

	public static @View int testByteArrayThenIncrease() {
		StorageTreeByteArray array = new StorageTreeByteArray(100);
		Random random = new Random(12345L);

		for (byte i = 1; i <= 50; i++) {
			int index;

			do {
				index = random.nextInt(array.length);
			}
			while (array.get(index) != 0);
			
			array.set(index, i);
		}

		for (int i = 0; i < array.length; i++)
			array.set(i, (byte) (array.get(i) + 1));

		return array.stream().sum();
	}
}